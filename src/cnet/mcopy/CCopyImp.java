//============================================================================
//
//	Copyright (c)2016 clusternet.org - All Rights Reserved.
//
//----------------------------------------------------------------------------
//
//	Description: CCopyImp
//
//  History: 
//	09/02/2016 Create
//
// 	Authors: 
//		 Alejandro Garcia Dominguez (alejandro.garcia.dominguez@gmail.com)
//		
//
//  This file is part of ClusterNet 
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//	http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//----------------------------------------------------------------------------

package cnet.mcopy;

import java.io.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.TreeMap;

import javax.crypto.NoSuchPaddingException;

import cnet.*;

import java.util.Iterator;

/**
 * Protocol CCopy (Cluster Copy) version 1.0
 * This class describe the structure of the protocol CCopy. it implement the 
 * functionality also.
 */
public class CCopyImp extends Thread
  implements ClusterNetRxDataListener, ClusterNetMemberInputStreamListener
    ,ClusterNetConnectionListener, ClusterNetGroupListener, ClusterNetMemberListener
{

  public final static int CCOPY_CONNECTING = 0x01;
  public final static int CCOPY_CONNECTED = 0x02;
  public final static int CCOPY_STARTRECEIVE = 0x03;
  public final static int CCOPY_STARTSEND = 0x04;
  public final static int CCOPY_END = 0x05;
  public final static int CCOPY_CLOSE = 0x06;
  public final static int CCOPY_CANCEL = 0x07;
  
	
   /** MAGIC */
  public final static int MAGIC = 0x6DED757B;

  /** VERSION 1*/
  public final static int VERSION = 0x01;

  /** Tamaño del array de Transmisión/Recepcion */
  public static final int TAMAÑO_ARRAY_BYTES = 1024 * 2;

  /** Fichero */
  File file = null;

  /** Flujo de salida del Fichero */
  private FileOutputStream fileOutputStream = null;

  /** TAmaño del Fichero */
  long lFileSize = 0;

  /** Nombre del Fichero*/
  String sFileName = null;

  /** Flag de lectura de datos del socket */
  private boolean bLeer = false;

  /** Semáforo binario de ESPERA*/
  private Semaforo semaforoFin = null;

  /** Semáforo binario para EMISION*/
  private Semaforo semaforoEmision = null;

  /** Semáforo binario para RECEPCION*/
  private Semaforo semaforoRecepcion = null;

    /** Flag de parada de la transferencia */
  private boolean bStop = false;

  /** Flujo de salida */
  private ClusterNetOutputStream out = null;

  /** Flujo de entrada Multicast*/
  private ClusterNetInputStream inMcast = null;

  /** Nueva Línea */
  private static final String newline = "\n";

  /** Address dirIPMcast */
  private Address dirIPMcast = null;

  /** Address dirIPInterfaz */
  private Address dirIPInterfaz = null;

  /** clave */
  private char[] clave = null;

  /**TTL sesion */
  private int TTLSesion = 0;

  /** Registro_ID_Socket */
  private RegistroID_Socket_Buffer reg = null;

 

  /**
   * TreeMap de Threads ThreadRecepcion. KEY= ClusterMemberInputStream. VALUE=FileRecepcion
   *  UTILIZADO EN MODO FIABLE.
   */
  private TreeMap treemapID_SocketInputStream = null;

  /**
   * TreeMap de ID_SOCKETS. KEY= ID_SOCKET. VALUE= Filerecepcion
   * UTILIZADO EN MODO NO_FIABLE.
   */
  private TreeMap treemapID_Socket = null;

  /** Socket Multicast Fiable*/
  private ClusterNet socket = null;

  /** Modo de fiabilidad del socket*/
  private int modo = 0;

  /** Socket Multicast No Fiable*/
  private ClusterNet datagramSocket = null;

  /** Flag de inicio del thread */
  private boolean runFlag = true;

  /** Ratio ed transferencia */
  private long lRatio = 0;
  
  /** Interface reference */
  public InterfaceCCopy intCCopy;
  


 //==========================================================================
 /**
  * Constructor
  */
  public CCopyImp(InterfaceCCopy intCCopy) throws IOException
  {
    super("CCopyImp");
    this.intCCopy = intCCopy;
    
    setDaemon(true);

    try
    {
      //Crear semáforos
      semaforoFin = new Semaforo(true,1);
      semaforoEmision = new Semaforo(true,1);
      semaforoRecepcion = new Semaforo(true,1);
    }
    catch(ClusterNetInvalidParameterException e){;}

  }


 //==========================================================================
 /**
  * Método Run()
  */
 public void run()
 {
   Cipher cipher = null;
   boolean bCipher = false;
  
   try
   {
     runFlag = true;

       // Log.log("CRIPTOGRAFIA--> CLAVE:"+clave ,"");

     if(!(new String(clave).equals("")))
     {
        Log.log("CRYPTO--> Key:"+clave ,"");
        try {
			cipher = Cipher.getInstance( clave);
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException
				| NoSuchPaddingException | InvalidAlgorithmParameterException e) {

			intCCopy.errorLog(e.getMessage());
			e.printStackTrace();
			cipher = null ;
		}

        if(cipher == null)
        {
          //TODO runGUI.getGUI().error("Could not create the crypto objects.");
        	intCCopy.errorLog("Could not create the crypto objects.");
          return;
        }
        else
         bCipher = true;
     }

    //Establecer nivel de depuracion
    ClusterNet.setLogLevel(Log.ACK | Log.HACK | Log.NACK | Log.HNACK | Log.HSACK | Log.TPDU_RTX);


     //1.- Crear el socket..
     if (modo == ClusterNet.MODE_DELAYED_RELIABLE || modo == ClusterNet.MODE_RELIABLE)
     {
        //Log.log("MODO ClusterNet ClusterNet FIABLE /RETRASADO","");
       //Iniciar Logo...
       // runGUI.getGUI().logoOn();
    	 intCCopy.statusClusterConnection(new CCopyEvent(CCOPY_CONNECTING,null,-1,-1,null));
    	 
       if(bCipher)
         socket = new ClusterNet(dirIPMcast,dirIPInterfaz,(byte)TTLSesion,modo,this,cipher.getCipher(),cipher.getUncipher());
       else
         socket = new ClusterNet(dirIPMcast,dirIPInterfaz,(byte)TTLSesion,modo,this);


       //Registrar listener eventos...
       //this.socket.addPTMFConexionListener(this);
       this.socket.addGroupListener(this);
       this.socket.addMemberListener(this);

       //Obtener idgls e id_scoket
       intCCopy.notifyGroups(this.socket.getNumGroups());
       //ftp.idgls = this.socket.getNumGroups();
       
       intCCopy.notifyMembers(this.socket.getNumMembers());
       //ftp.id_sockets = this.socket.getNumMembers();
       //ftp.getJLabelIDGLs().setText("IDGLS: "+ftp.idgls);
       //ftp.getJLabelID_Sockets().setText("ID_Sockets: "+ftp.id_sockets);

       //Obtener los grupos conocidos...
       TreeMap treemapIDGL = this.socket.getGroups();
   
       //TODO
       /*Iterator iterator = treemapIDGL.values().iterator();

       while(iterator.hasNext())
       {
          RegistroIDGL_TreeMap regIDGL = (RegistroIDGL_TreeMap) iterator.next();

          ftp.getJFrame().jTreeInformacion.addIDGL(regIDGL.getIDGL());
       }
		*/
       intCCopy.notifyGroups(treemapIDGL);
       
       
       //Obtener los Miembros de este grupo...
       TreeMap treemapIDSocket = this.socket.getMembers();
       //TODO
       /*
       Iterator iteratorSockets = treemapIDSocket.keySet().iterator();

       while(iteratorSockets.hasNext())
       {
          ClusterMemberID idSocket = (ClusterMemberID) iteratorSockets.next();

          ftp.getJFrame().jTreeInformacion.addID_Socket(idSocket);
       }
		*/
       intCCopy.notifyMembers(treemapIDSocket);

       //Obtener Flujos de Entrada y de Salida
       this.out = this.getSocket().getClusterOutputStream();
       this.inMcast = this.getSocket().getClusterInputStream();

       
       
       if(runFlag==false)
         return;

     }
     else
     {
       //Iniciar Logo...
       //TODO  ftp.logoOn();
       intCCopy.statusClusterConnection(new CCopyEvent(CCOPY_CONNECTING,null,-1,-1,null));

       if(bCipher)
         datagramSocket = new ClusterNet(dirIPMcast,dirIPInterfaz,(byte)TTLSesion,modo,this,cipher.getCipher(),cipher.getUncipher());
       else
         datagramSocket = new ClusterNet(dirIPMcast,dirIPInterfaz,(byte)TTLSesion,modo,this,null,null);

       if(runFlag==false)
          return;

       //Registrar listeners eventos...
       datagramSocket.addConnectionListener(this);
            

     }

     if(runFlag==false)
        return;

     //Conectado¡¡¡
    //TODO ftp.insertInformacionString("Conexión Multicast establecida con "+dirIPMcast+ "TTL= "+TTLSesion);
     intCCopy.statusClusterConnection(new CCopyEvent(CCOPY_CONNECTED,null,-1,-1,null));

     if(runFlag==false)
        return;


     // ENVIAR/RECIBIR FICHEROS...
     if( intCCopy.isSender()) //TODO
     {

        this.socket.setRatioTx(lRatio);
        //ftp.insertInformacionString("Ratio de transferencia: "+lRatio/1024+" KB/Seg");


        //Cerrar RECEPCION¡¡¡¡
        if(socket!=null)
          socket.disableRx();

        //Cerrar RECEPCION¡¡¡¡
        if(datagramSocket!=null)
          datagramSocket.disableRx();
        
       //TODO ftp.getJFrame().jPanelTransmisor.setEnabled(true);
       //TODO ftp.getJFrame().jPanelReceptor.setEnabled(false);

        this.waitSendFiles();
     }
     else
     {
    	//TODO ftp.getJFrame().jPanelTransmisor.setEnabled(false);
    	//TODO ftp.getJFrame().jPanelReceptor.setEnabled(true);

        this.waitReceiveFiles();
     }

     return;
  }
  catch(ClusterNetInvalidParameterException e)
  {
     intCCopy.errorLog(e.getMessage());
     intCCopy.statusClusterConnection(new CCopyEvent(CCOPY_END,null,-1,-1,null));
  }
  catch(ClusterNetExcepcion e)
  {
	  intCCopy.errorLog(e.getMessage());
	  intCCopy.statusClusterConnection(new CCopyEvent(CCOPY_END,null,-1,-1,null));
  }
  catch(IOException e)
  {
	  intCCopy.errorLog(e.getMessage());
	  intCCopy.statusClusterConnection(new CCopyEvent(CCOPY_END,null,-1,-1,null));
  }


  //Limpiar....
  finally
  {
      //Cerrar el Socket
      close();
      Log.log("END CCopyImp","");
  }
 }

 //==========================================================================
 /**
  * conectar
  */
  public void conectar(Address dirIPMcast,
      Address dirIPInterfaz,int TTLSesion,long lRatio, int modo, char[] clave)
  {
    this.dirIPMcast = dirIPMcast;
    this.dirIPInterfaz = dirIPInterfaz;
    this.modo = modo;
    this.TTLSesion = TTLSesion;
    this.clave = clave;
    this.lRatio = lRatio;

    // Iniciar el thread
    this.start();
 }

 //==========================================================================
 /**
  * getSocket()
  */
 ClusterNet getSocket(){ return this.socket;}

 //==========================================================================
 /**
  * getDatagramSocket()
  */
 ClusterNet getDatagramSocket(){ return this.datagramSocket;}



  //==========================================================================
 /**
  * getModo()
  */
 int getModo(){ return this.modo;}

 //==========================================================================
 /**
  * getMulticastOutputStream()
  */
 //ClusterNetOutputStream getMulticastOutputStream(){ return this.out;}

 //==========================================================================
 /**
  * Cerrar el Socket
  */
 void close()
 {
   try
    {
      //Cerrar el Socket...
      if (modo  == ClusterNet.MODE_DELAYED_RELIABLE || modo  == ClusterNet.MODE_RELIABLE)
        {
         if(socket!= null)
         {
            socket.endTx();
            socket.close(ClusterNet.CLOSE_STABLE);
         }
        }
      else
        if(datagramSocket!= null)
            datagramSocket.close();

         //if(semaforoFin == null)

    // ?¿?¿?¿?¿??¿?¿ ThreadRecepcion.interrupted();*******************-----

     //Despertar...
     if (semaforoFin != null)
       semaforoFin.up();

     if(semaforoEmision != null)
       semaforoEmision.up();

     if(semaforoRecepcion != null)
       semaforoEmision.up();


    }
    catch(ClusterNetExcepcion e)
    {
            endCConnection(e);
    }
 }

 //==========================================================================
 /**
  * End CCopy isntance
  */
  void endCConnection(IOException ioe)
  {
     //TODO runGUI ftp = runGUI.getGUI();
	  /*
      ftp.insertStringJTextPane(ftp.getJTextPaneInformacion(),ioe.getMessage(),"error");
      ftp.insertInformacionString("Conexión Cerrada");
      runGUI.getGUI().logoOff();
      */
	  intCCopy.errorLog(ioe.getMessage());
	  intCCopy.statusClusterConnection(new CCopyEvent(CCOPY_CLOSE,null,-1,-1,null));
      this.runFlag = false;
  }

 //==========================================================================
 /**
  * Método stopThread()
  */
 public void stopThread()
 {
   this.runFlag = false;

   if(semaforoRecepcion!= null)
     semaforoRecepcion.up();

     if (semaforoFin != null)
       semaforoFin.up();

     if(semaforoEmision != null)
       semaforoEmision.up();

     if(semaforoRecepcion != null)
       semaforoEmision.up();


  //if( this.protocoloFTPMulticast!= null)
  // this.protocoloFTPMulticast.close();
 }

 //==========================================================================
 /**
  * Indica si la sesión está activa o desactivada.
  * @return true si la sesión está activa, false en caso contrario
  */
 public boolean esActiva()
 {
   return this.runFlag;
 }



 //==========================================================================
 /**
  * Implementación de la interfaz ClusterNetConnectionListener
  */
 public void actionNewConnection(ClusterNetEventConnection evento)
 {
   // runGUI ftp = runGUI.getGUI();
    //Log.log("actionPTMFConexion","");
    //Log.log("actionPTMFConexion: "+evento.getString(),"");

    //if( ftp != null && runFlag==true)
    //{
       //TODO ftp.insertInformacionString(evento.getString());
	 intCCopy.infoLog(evento.getString());
      
       //ftp.insertStringJTextPane(" ","icono_informacion");
       //ftp.insertInformacionString(evento.getString());
    //}
 }

 //==========================================================================

 //==========================================================================
 /**
  * Espera para la emisión de ficheros...
  */
 void waitSendFiles() throws IOException
 {
         //BUCLE PRINCIPAL
     while(this.esActiva())
     {
          //Si NO HAY NADA QUE EMITIR--> DORMIR HASTA QUE LO HAYA.
          if(file== null || this.bStop == true)
           this.semaforoEmision.down();

          //Verificar si se ha cerrado la conexión...
          if(!this.esActiva())
          {
            limpiar();
            return;
          }

          //Enviar fichero....
          FileEmision fileEmision = new FileEmision(this,this.file,intCCopy);
          fileEmision.sendFile();

          if(!this.esActiva())
          {
            limpiar();
            return;
          }

          this.file = null;
   }
 }

 //==========================================================================
 /**
  * Espera para la recepción de ficheros...
  */
 void waitReceiveFiles() throws IOException
 {
    if(this.getModo() == ClusterNet.MODE_DELAYED_RELIABLE || this.getModo()  == ClusterNet.MODE_RELIABLE)
    {
      //Registrar ID_SocketInputStreamListener
      this.getSocket().getClusterInputStream().addPTMFID_SocketInputStreamListener(this);

    }
    else
    {
      //Registrar PTMFDatosRecibidos
      this.getDatagramSocket().addRxDataListener(this);

      //Crear el Treemap si es NULL
      if(this.treemapID_Socket == null)
       this.treemapID_Socket = new TreeMap();
    }

    //Información..
    intCCopy.infoLog("Waiting for file reception...");
    //runGUI.getGUI().insertRecepcionString("Esperando recepción de ficheros...","icono_informacion");

    //***** BUCLE PRINCIPAL *****
    while(this.esActiva())
    {

      if(this.getModo() != ClusterNet.MODE_DELAYED_RELIABLE && this.getModo()  != ClusterNet.MODE_RELIABLE)
      {
        //MODO NO-FIABLE
        //ESPERAR A QUE HAYA DATOS..
        this.semaforoRecepcion.down();

        //Leer Bytes NO FIABLE...
        recibirDatagrama();
      }
      else
      { //MODO FIABLE
        //ESPERAR A QUE FINALICE EL THREAD SESION MULTICAST,
        // LA RECEPCION SE HACE DE FORMA ASÍNCRONA CON EL LISTENER ID_SOCKETINPUTSTREAM...
        while(this.esActiva())
          ClusterTimer.sleep(500);
      }
    }//FIN WHILE PRINCIPAL
 }


 //==========================================================================
 /**
  * recibirDatagrama();
  */
 private void recibirDatagrama() throws IOException
 {
   byte[] bytes = new byte[this.TAMAÑO_ARRAY_BYTES];
   String sFileName = null;
   long lFileSize = 0;

   //1.- **Leer DATOS**
   RegistroID_Socket_Buffer reg = this.getDatagramSocket().receive();

   //Crear el Treemap si es NULL
   if(this.treemapID_Socket == null)
    this.treemapID_Socket = new TreeMap();


   // SI EL ClusterMemberID no está en el treemap, Significa CONEXIÓN NUEVA....
   if(!this.treemapID_Socket.containsKey(reg.getID_Socket()))
   {
       Log.log("NUEVO ClusterMemberID: "+reg.getID_Socket(),"");
       Buffer buf = reg.getBuffer();

       //Comprobar IDFTP
       if (!FileRecepcion.parseIDFTPMulticast(buf))
        return;

       //Comprobar Tamaño
       lFileSize = FileRecepcion.parseFileSize(buf);
       if(lFileSize <= 0)
        return;

       //Comprobar FileName
       sFileName = FileRecepcion.parseFileName(buf);
       if( sFileName == null)
        return;

       // protocoloFTPMulticast.getGUI().insertStringJTextPane(" ","icono_entrada");
       //runGUI.getGUI().insertRecepcionString("Iniciando la recepción... de "+sFileName,null);
       intCCopy.statusClusterConnection(new CCopyEvent(CCOPY_STARTRECEIVE,sFileName,lFileSize,0,null));
       intCCopy.infoLog("<- file "+sFileName+ " from "+reg.getID_Socket()+ "size:"+lFileSize);
       
       //this.getGUI().insertStringJTextPane(" ","icono_entrada");
       //runGUI.getGUI().insertRecepcionString("Recibiendo fichero: "+sFileName+" del emisor: "+reg.getID_Socket(),null);
       //this.getGUI().insertStringJTextPane(" ","icono_entrada");
       //runGUI.getGUI().insertRecepcionString("Tamaño: "+lFileSize,null);


       //Nuevo FileRecepcion...
       FileRecepcion fileRecepcion  = new FileRecepcion(this,reg.getID_Socket());
       //Recibir fichero
       fileRecepcion.receiveFile(lFileSize,sFileName);

       //Añadir nuevo FileRecepcion al treemap...
       this.treemapID_Socket.put(reg.getID_Socket(),fileRecepcion);

   }
   else
   {
      //Obtener FileRecepcion...
      FileRecepcion fileRecepcion = (FileRecepcion) this.treemapID_Socket.get(reg.getID_Socket());

      //Añadir los bytes leídos...
      fileRecepcion.addBytes(reg.getBuffer().getBuffer(),reg.getBuffer().getLength());

      //FALTA FALTA FALTA FALTA FALTA FALTA FALTA FALTA FALTA FALTA
      //FALTA FALTA FALTA FALTA FALTA FALTA FALTA FALTA FALTA FALTA
      //FALTA FALTA FALTA FALTA FALTA FALTA FALTA FALTA

      //ALES FALYA CONTENPLAR FIN DE FLUJO ¿?¿?¿¿?¿????¿?¿? *****************---------

      //  FALTA FALTA FALTA FALTA FALTA FALTA FALTA FALTA FALTA
      //FALTA FALTA FALTA FALTA FALTA FALTA FALTA FALTA FALTA FALTA

   }

 }


 //==========================================================================
 /**
  * Eliminar un FileRecepcion. MODO NO FIABLE.
  * @param ClusterMemberID
  */
 void removeFileRecepcion(ClusterMemberID id_socket)
 {
    this.treemapID_Socket.remove(id_socket);
 }

 //==========================================================================
 /**
  * Eliminar un FileRecepcion. MODO FIABLE.
  */
  void removeFileRecepcion(ClusterMemberInputStream idIn)
  {
    //Log.log("Remove fileRecepcion: "+idIn,"");

    if (idIn == null)
      Log.log("idIN ES NULLLLL¡¡¡¡","");
    if (this.treemapID_SocketInputStream!= null)
      this.treemapID_SocketInputStream.remove(idIn);
  }


 

 //==========================================================================
 /**
  * Mensaje de advertencia--> No se puede escribir en el fichero.
  */
 private void mensajeErrorEscritura()
 {
    //JOptionPane.showMessageDialog(null,"No se puede escribir en el fichero: "+sFileName+newline+"no se tiene permiso de escritura"+newline+"Verifique los permisos de escritura"+newline+" y que tiene suficiente privilegio para escribir." ,
	//			    "Error Escritura",JOptionPane.ERROR_MESSAGE);
	  intCCopy.errorLog("I can't write the file: "+sFileName+newline+". Perhaps i don't have write permission"+newline+"Verifique los permisos de escritura."+newline);
 }

 //==========================================================================
 /**
  * Mensaje de advertencia--> Error Escribiendo
  */
 private void mensajeErrorEscribiendo(String sError)
 {
    //JOptionPane.showMessageDialog(null,"Se ha producido un error mientras se intentaba escribir en el fichero"+newline+sFileName+newline+"El error es el siguiente:"+sError ,
	//			    "Error Escritura",JOptionPane.ERROR_MESSAGE);
    
    intCCopy.errorLog("An error happened while writing file "+newline+sFileName+newline+". The error reported is:"+sError);
 }





//==========================================================================
 /**
  * sendFile envía el fichero sFile por el canal Multicast.
  * @param file el fichero que se desea transmitir por Multicast
  * @param icon Icono representativo del fichero
  * @return Boolean. true si se ha iniciado la transferencia, false en caso contrario.
  */
 public boolean sendFile(File file)
 {
    if (!esActiva())
      return false;

    if(this.file!=null)
    {
      errorFile("There is already a transfer. Please wait it finished to start a new one.");
      return false;
    }

    //Asignar..
    this.file = file;
    //this.icon = icon;

    //Enviar...
    this.bStop = false;
    this.semaforoEmision.up();
    return true;
 }




 //==========================================================================
 /**
  * Implementación de la interfaz ClusterNetGroupListener
  * para la recepción de datos en modo NO_FIABLE
  */
 public void actionPTMFIDGL(ClusterNetEventGroup evento)
 {
 //   runGUI ftp = runGUI.getGUI();

   // if( evento.esAñadido())
   // {
     
       intCCopy.notifyGroups(this.socket.getNumGroups());
       //Obtener los grupos conocidos...
       TreeMap treemapIDGL = this.socket.getGroups();
       
       //Añadir el ClusterGroupID al árbol de información
     //  ftp.getJFrame().jTreeInformacion.addIDGL(evento.getIDGL());
       intCCopy.notifyGroups(treemapIDGL);

       //.getGUI().insertInformacionString("IDGLS: "+runGUI.getGUI().idgls);
       
       //runGUI.getGUI().insertInformacionString("Nuevo ClusterGroupID: "+evento.getIDGL());

   /* }
    else
    {
     // ftp.idgls = this.socket.getNumGroups();
    	intCCopy.notifyMembers(this.socket.getNumMembers());
      //Eliminar IDGLs del árbol
      // ftp.getJFrame().jTreeInformacion.removeIDGL(evento.getIDGL());

       runGUI.getGUI().insertInformacionString("IDGLS: "+runGUI.getGUI().idgls);
       runGUI.getGUI().insertInformacionString("ClusterGroupID eliminado: "+evento.getIDGL());
    }*/
 }

  //==========================================================================
 /**
  * Implementación de la interfaz ClusterNetGroupListener
  * para la recepción de datos en modo NO_FIABLE
  */
 public void actionID_Socket(ClusterNetEventMember evento)
 {
   // runGUI ftp = runGUI.getGUI();

    /*if( evento.esAñadido())
    {
      ftp.id_sockets = this.socket.getNumMembers();

      //Añadir el ClusterMemberID al árbol de información
      ftp.getJFrame().jTreeInformacion.addID_Socket(evento.getID_Socket());

       runGUI.getGUI().insertInformacionString("ID_Sockets: "+runGUI.getGUI().id_sockets);
       runGUI.getGUI().insertInformacionString("Nuevo ClusterMemberID: "+evento.getID_Socket());
    }
    else
    {
      ftp.id_sockets = this.socket.getNumMembers();

      //Añadir el ClusterMemberID al árbol de información
      ftp.getJFrame().jTreeInformacion.removeIDSocket(evento.getID_Socket());

      ftp.insertInformacionString("ID_Sockets: "+runGUI.getGUI().id_sockets);
      ftp.insertInformacionString("ClusterMemberID eliminado: "+evento.getID_Socket());
    }*/
	 
	 intCCopy.notifyMembers(this.socket.getNumMembers());

	 //Obtener los grupos conocidos...
     TreeMap treemapMembers = this.socket.getMembers();
     
     intCCopy.notifyMembers(treemapMembers);


 }

 //==========================================================================
 /**
  * Implementación de la interfaz ClusterNetRxDataListener
  * para la recepción de datos en modo NO_FIABLE
  */
 public void actionRxData(ClusterNetEventNewData evento)
 {
    // Hay datos, despertar si estaba dormido
    this.bLeer = true;
    this.semaforoRecepcion.up();
 }



 //==========================================================================
 /**
  * ClusterNetEventMemberInputStream
  */
 public void actionMemberInputStream(ClusterNetEventMemberInputStream evento)
 {
   //Log.log("\n\nNUEVO ID_SOCKETINPUTSTREAM","");

   //Crear TreeMap threads de recepcion ...
   if ( treemapID_SocketInputStream == null)
      this.treemapID_SocketInputStream = new TreeMap();

   ClusterMemberInputStream idIn = evento.getID_SocketInputStream();
   if (idIn == null)
   {
    //Log.log("\n\nNUEVO ID_SOCKETINPUTSTREAM: NULL","");
    return;
   }

   //Log.log("\n\n ID_SOCKETINPUTSTREAM: --->OK","");

   if( !this.treemapID_SocketInputStream.containsKey(idIn))
   {
        // Log.log("\n\n PARA CREAR THREADS","");

       try
       {
          //PONER --> Obtener lFilesize y sFileName antes de crear FileRecepcion
          FileRecepcion fileRecepcion = new FileRecepcion(this,idIn);
          //Log.log("\n\nCREANDO THREAD Filerecepcion","");

          this.treemapID_SocketInputStream.put(idIn,fileRecepcion);

          //Iniciar thread...
          fileRecepcion.start();
       }
       catch(IOException ioe)
       {
         intCCopy.errorLog(ioe.toString());
       }
   }
 }
 //==========================================================================
 /**
  * Error Abriendo el Fichero.
  * @param sCadenaInformativa
  */
 private void errorFile(String sCadenaInformativa)
 {

	 
	/* JOptionPane.showMessageDialog(null,sCadenaInformativa,
				    "Error", JOptionPane.ERROR_MESSAGE);*/

  intCCopy.errorLog(sCadenaInformativa);
 }





 //==========================================================================
  /**
   *  Limpiar variables....
   */
  private void limpiar()
  {
     this.file = null;
     this.bStop = true;
  }




}