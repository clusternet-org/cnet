/**
  
  Copyright (c) 2000-2014 . All Rights Reserved.
  @Autor: Alejandro García Domínguez alejandro.garcia.dominguez@gmail.com   alejandro@iacobus.com
         Antonio Berrocal Piris antonioberrocalpiris@gmail.com
 
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations 
  */

package org.clusternet.chat;



//IO
import java.io.Writer;

import org.clusternet.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.OutputStream;
import java.io.BufferedWriter;
import java.io.InputStreamReader;


 //==========================================================================
 /**
  * Clase ThreadSesionMulticast. Gestiona la conexión Multicast.
  */
 public class ThreadSesionMulticast extends Thread
 implements ClusterNetConnectionListener, ClusterNetRxDataListener, ClusterNetGroupListener, ClusterNetMemberListener
 {

  /** Socket Multicast Fiable*/
  private ClusterNet socket = null;

  /** Modo de fiabilidad del socket*/
  private int modo = 0;

  /** Socket Multicast No Fiable*/
  private ClusterNet datagramSocket = null;

  /** cChat */
  private cChat chat = null;

  /** Flag de lectura de datos del socket */
  private boolean bLeer = false;

  /** Semáforo binario */
  private Semaforo semaforo = null;

  /** Flag de inicio del thread */
  private boolean runFlag = false;

  /** Flujo de salida */
  private Writer out = null;

  /** Flujo de entrada Multicast*/
  private ClusterNetInputStream inMcast = null;

  /** Flujo de entrada*/
  //private BufferedReader in = null;

  /** Flujo de entrada ID_SocketInputStream */
  private ID_SocketInputStream id_socketIn = null;

  /** Nueva Línea */
  private String newline = "\n";

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

  /** Flag de Sesion Aciva */
  private boolean bActiva = false;

  /** Numero de IDGLs */
  private int idgls = 0;

  /** Numero de ID_Sockets */
  private int id_sockets = 0;

 //==========================================================================
 /**
  * Constructor
  */
  public ThreadSesionMulticast(cChat chat)
  {
    super();

    this.chat = chat;
    setDaemon(true);
  }

 //==========================================================================
 /**
  * Conectar. Inicia el Thread y establece una sesión Multicast
  * con los parámetros establecidos....
  */
  public void conectar(Address dirIPMcast,
      Address dirIPInterfaz,int TTLSesion,int modo, char[] clave)
  {
    this.dirIPMcast = dirIPMcast;
    this.dirIPInterfaz = dirIPInterfaz;
    this.modo = modo;
    this.TTLSesion = TTLSesion;
    this.clave = clave;
    start();
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


     if(!(new String(clave).equals("")))
     {
        Log.log("CRIPTOGRAFIA--> CLAVE:"+clave ,"");
        cipher = Cipher.getInstance(this.chat,clave);

        if(cipher == null)
        {
          this.chat.error("No se ha podido crear los objetos de Cifrado.");
          return;
        }
        else
         bCipher = true;
     }

     //Crear el socket..
     if (modo == ClusterNet.MODE_DELAYED_RELIABLE || modo == ClusterNet.MODE_RELIABLE)
     {
       //Iniciar Logo...
       chat.logoOn();

       Log.log("Criptografia --> "+bCipher,"");
       if(cipher != null && cipher.getCipher()!=null)
         Log.log("Codificador: "+cipher.getCipher(),"");
       if(cipher != null && cipher.getUncipher()!=null)
         Log.log("Descodificador: "+cipher.getUncipher(),"");

       if(bCipher)
         socket = new ClusterNet(dirIPMcast,dirIPInterfaz,(byte)TTLSesion,modo,this,cipher.getCipher(),cipher.getUncipher());
       else
         socket = new ClusterNet(dirIPMcast,dirIPInterfaz,(byte)TTLSesion,modo,this);


       //Registrar listener eventos...
       //this.socket.addPTMFConexionListener(this);
       this.socket.addGroupListener(this);
       this.socket.addMemberListener(this);

       //Obtener idgls e id_scoket
       this.idgls = this.socket.getNumGroups();
       this.id_sockets = this.socket.getNumMembers();
       this.chat.getJLabelIDGLs().setText("Groups: "+this.idgls);
       this.chat.getJLabelID_Sockets().setText("Members: "+this.id_sockets);

       //Registrar PTMFDatosRecibidos
       socket.getClusterInputStream().addPTMFDatosRecibidosListener(this);
     }
     else
     {
       //Iniciar Logo...
       chat.logoOn();

       if(bCipher)
         datagramSocket = new ClusterNet(dirIPMcast,dirIPInterfaz,(byte)TTLSesion,modo,this,cipher.getCipher(),cipher.getUncipher());
       else
          datagramSocket = new ClusterNet(dirIPMcast,dirIPInterfaz,(byte)TTLSesion,modo,this,null,null);

       //Registrar PTMFConexion
       //datagramSocket.addPTMFConexionListener(this);

       //Registrar PTMFDatosRecibidos
       datagramSocket.addRxDataListener(this);
     }

     bActiva = true;

     //Conectado¡¡¡
     chat.getJLabelInformacion().setText("Conexión Multicast establecida con "+dirIPMcast+ "TTL: "+TTLSesion);

     //Crear semáforo
     semaforo = new Semaforo(true,1);


     //Obtener flujos de entrada/salida en los modos FIABLE
     if (modo == ClusterNet.MODE_RELIABLE || modo == ClusterNet.MODE_DELAYED_RELIABLE)
     {
       out = new BufferedWriter (new OutputStreamWriter((OutputStream)socket.getClusterOutputStream()),255);
       inMcast = socket.getClusterInputStream();
     }


     //Bucle principal....
     while(runFlag)
     {
      //Dormir si no hay que leer
      if(!bLeer)
         semaforo.down();
      else
      {

       bLeer = false;
       Log.log("DEPU 1","");

       if (modo == ClusterNet.MODE_DELAYED_RELIABLE || modo == ClusterNet.MODE_RELIABLE)
       {
           //Mientras haya datos que leer
           while(this.inMcast.available()>0)
           {

             //Obtener el Flujo de entrada....
             id_socketIn = inMcast.nextID_SocketInputStream();

             if(id_socketIn != null && (id_socketIn.available() > 0))
             {
                chat.insertStringJTextPaneSuperior(" ","icono_entrada");
                chat.insertStringJTextPaneSuperior(id_socketIn.getID_Socket().toString(),"entrada");
                byte[] bytes = new byte[id_socketIn.available()];
                id_socketIn.read(bytes);
                chat.insertStringJTextPaneSuperior(new String(bytes),"entrada");
             }
          }
       }
       else
       {
           //Mientras haya datos que leer
           while(datagramSocket.available()>0)
           {
             reg = datagramSocket.receive();
              if(reg.esFinTransmision())
                break;
             chat.insertStringJTextPaneSuperior(" ","icono_entrada");
             chat.insertStringJTextPaneSuperior(reg.getID_Socket().toString()+": "+new String(reg.getBuffer().getBuffer()),"entrada");
           }
       }
      }
     }

  }
  catch(ClusterNetInvalidParameterException e)
  {
    finalizar();
    chat.error(e.getMessage());
  }
  catch(ClusterNetExcepcion e)
  {
    finalizar();
    chat.error(e.getMessage());
  }
  catch(IOException e)
  {
    finalizar();
    chat.error(e.getMessage());
  }

  finally
  {
     //Cerrar el Socket
     close();

     //Limpiar ...
     finalizar();
  }
 }

 //==========================================================================
 /**
  * Cerrar el Socket
  */
 void close()
 {
   try
    {
      //Cerrar el Socket...
      if (modo == ClusterNet.MODE_DELAYED_RELIABLE || modo == ClusterNet.MODE_RELIABLE)
      {
        if(socket!= null)
          socket.close(true);
      }
      else
      {
        if(datagramSocket!= null)
          datagramSocket.close();
      }

      socket =  null;
    }
    catch(ClusterNetExcepcion e)
    {
        finalizar();
    }
 }

 //==========================================================================
 /**
  * Finalizar
  */
 public void finalizar()
 {
     runFlag = false;
      bActiva = false;
     //Parar Logo...
     chat.logoOff();
 }

 //==========================================================================
 /**
  * Método stopThread()
  */
 public void stopThread()
 {
   this.runFlag = false;
   if(semaforo != null)
   {
     //Despertar...
     semaforo.up();
   }
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
  * sendStream envía la cadena pasada como argumento por el canal Multicast.
  * @exception IOException se lanza si ocurre un error
  */
 public void sendString(String cadena) throws IOException
 {
     if (cadena == null) return;

    //Log.log("SendSetring. tamaño"+cadena.length(),"");
     if ((this.modo == ClusterNet.MODE_RELIABLE)|| (this.modo ==ClusterNet.MODE_DELAYED_RELIABLE))
    {
      //Obtener el flujo de salida multicast...
      ClusterNetOutputStream out = this.socket.getClusterOutputStream();

      //Enviar los datos....
      out.write(cadena.getBytes());

    }
    else
    {
      this.datagramSocket.send(new Buffer(cadena.getBytes()));
    }
 }

 //==========================================================================
 /**
  * Implementación de la interfaz ClusterNetRxDataListener
  */
 public void actionPTMFDatosRecibidos(ClusterNetEventNewData evento)
 {
    // Hay datos, despertar al thread si estaba dormido
    this.bLeer = true;
    this.semaforo.up();
    Log.log("actionListener --> Datos Recibidos","");
 }

 //==========================================================================
 /**
  * Implementación de la interfaz ClusterNetConnectionListener
  */
 public void actionNewConnection(ClusterNetEventConecction evento)
 {
    if(chat != null)
    {
      chat.insertStringJTextPaneSuperior(" ","icono_informacion");
      chat.insertStringJTextPaneSuperior("CONEXIÓN: ","Member");
      chat.insertStringJTextPaneSuperior(evento.getString()+newline,"informacion"+newline);
    }
 }


 //==========================================================================
 /**
  * Implementación de la interfaz ClusterNetGroupListener
  * para la recepción de datos en modo NO_FIABLE
  */
 public void actionPTMFIDGL(ClusterNetEventGroup evento)
 {
    if( evento.esAñadido())
    {
     this.idgls = this.socket.getNumGroups();
     this.chat.getJLabelIDGLs().setText("Groups: "+this.idgls);
     this.chat.insertStringJTextPaneSuperior(" ","icono_informacion");
     this.chat.insertStringJTextPaneSuperior("Notificación nuevo grupo: "+evento.getIDGL()+newline,"informacion");

    }
    else
    {
     this.idgls = this.socket.getNumGroups();
     this.chat.getJLabelIDGLs().setText("Groups: "+this.idgls);
     this.chat.insertStringJTextPaneSuperior(" ","icono_informacion");
     this.chat.insertStringJTextPaneSuperior("Grupo eliminado: "+evento.getIDGL()+newline,"informacion");
    }
 }

  //==========================================================================
 /**
  * Implementación de la interfaz ClusterNetGroupListener
  * para la recepción de datos en modo NO_FIABLE
  */
 public void actionID_Socket(ClusterNetEventMember evento)
 {
    if( evento.esAñadido())
    {
     this.id_sockets = this.socket.getNumMembers();
     this.chat.getJLabelID_Sockets().setText("Members: "+this.id_sockets);
     this.chat.insertStringJTextPaneSuperior(" ","icono_informacion");
     this.chat.insertStringJTextPaneSuperior("Notificación nuevo member: "+evento.getID_Socket()+newline,"informacion");
    }
    else
    {
     this.id_sockets = this.socket.getNumMembers();
     this.chat.getJLabelID_Sockets().setText("Members: "+this.id_sockets);
     this.chat.insertStringJTextPaneSuperior(" ","icono_informacion");
     this.chat.insertStringJTextPaneSuperior("Member eliminado: "+evento.getID_Socket()+newline,"informacion");
   }

 }

 }