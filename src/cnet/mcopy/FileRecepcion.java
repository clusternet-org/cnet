//============================================================================
//
//	Copyright (c)2016 clusternet.org - All Rights Reserved.
//
//----------------------------------------------------------------------------
//
//	Description: FileRecepcion
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

import javax.swing.JOptionPane;

import cnet.*;

import javax.swing.Icon;

import java.util.TreeMap;


 //==========================================================================
 /**
  * Class FileRecepcion.
  * All data relative to the file to be received using CCopy
  */
  class FileRecepcion extends Thread
  {

   /** Objeto File */
   private File file = null;

   /** Objeto FileOutputStream */
   private FileOutputStream fileOutputStream = null;

   /** Tamaño del Fichero */
   private long lFileSize = 0;

   /** Nombre del Fichero */
   private String sFileName = null;

   /** Diálogo de recepción */
  // private JDialogRecepcion jDialogRecepcion = null;

   /**  ClusterMemberInputStream */
   private ClusterMemberInputStream id_socketIn = null;

   /** ClusterMemberID */
   private ClusterMemberID id_socket = null;

   /** CCopyImp */
   private CCopyImp cCopyImp = null;

  /** Nueva Línea */
  private static final String newline = "\n";

  /** Bytes Leidos */
  private long lBytesLeidos = 0;
  
  /**Start time*/
  private long lTiempoInicio = 0;
  


  //==========================================================================
  /**
   * Constructor. It use reliable mode of clusterNet
   */
   public  FileRecepcion(CCopyImp cCopyImp, ClusterMemberID id_socket) throws IOException
   {
      super("FileRecepcion");
      this.setDaemon(true);

      this.id_socket = id_socket;
      this.cCopyImp = cCopyImp;
      lTiempoInicio = System.currentTimeMillis();

  }

  //==========================================================================
  /**
   * Constructor. Utilizado en MODO FIABBLE
   * @exception IOException Si no se puede crear el fichero. <br>
   *  MOSTRAR EL STRING DE LA EXCEPCION COMO UN ERROR.
   */
   public FileRecepcion(CCopyImp cCopyImp,ClusterMemberInputStream id_socketIn )
  throws IOException

   {
      super("FileRecepcion");
      this.setDaemon(true);

      this.cCopyImp = cCopyImp;
      this.id_socketIn = id_socketIn;
      lTiempoInicio = System.currentTimeMillis();
   }


  //==========================================================================
  /**
   * Método Run()
   */
  public void run()
  {
   //Log.log("\n\nFILERECEPCION","");
   try
   {
      //RECIBIR UN FICHERO
      this.receiveFile();

      if(this.id_socketIn!=null)
        this.cCopyImp.removeFileRecepcion(this.id_socketIn);


   }
   catch(ClusterNetInvalidParameterException e)
   {
     cCopyImp.intCCopy.errorLog(e.getMessage());
   }
   catch(ClusterNetExcepcion e)
   {
	   cCopyImp.intCCopy.errorLog(e.getMessage());
   }
   catch(IOException e)
   {
	   cCopyImp.intCCopy.errorLog(e.getMessage());
   }

   finally
   {

     Log.log("*.- FIN FileRecepcion FIABLE.","");
   }
  }

 //==========================================================================
 /**
  * Recibir UN FICHERO
  */
  private void receiveFile()  throws IOException
 {
     this.file = null;

     //Log.log("receiveFile()","");

     //---------------------------------
     //Recibir ID...
     if(!receiveIDFTPMulticast())
     {
        tirarBytes();
        return;
     }

     //Recibir FileSize...
     lFileSize = this.receiveFileSize();
     if(lFileSize <= 0)
     {
        tirarBytes();
        return;
     }

     //Recibir FileName...
     sFileName = this.receiveFileName();
     if (sFileName == null)
     {
        tirarBytes();
        return;
     }

      //Fichero temporal..
      this.file = File.createTempFile("cnet.mcopy"+System.currentTimeMillis(),".tmp");

      //Comprobar si ya existe.
      if(this.file.exists())
      {
        if(!this.file.delete())
            throw new IOException("The file could not deleted "+sFileName+newline+"1. Check you hace privilegies."+newline+"2.Check the file is not used by another application.");

      }

      //Crear el fichero...
      if(!this.file.createNewFile())
      {
          //mensajeErrorEscritura();
          throw new IOException("The file could not be closed "+sFileName+newline+"1. Check you hace privilegies."+newline+"2.Check the file is not used by another application.");

      }

      //Comprobar si se puede escribir.
      if(!this.file.canWrite())
      {
          //mensajeErrorEscritura();
          throw new IOException("The file could not be writed  "+sFileName+newline+"1. Check you hace privilegies."+newline+"2.Check the file is not used by another application.");
      }

      //Flujo de salida al fichero...
      this.fileOutputStream = new FileOutputStream(file);

     //Iniciar JDialogRecepcion
     //this.jDialogRecepcion = new JDialogRecepcion(this,null,"Receiving "+this.id_socketIn.getID_Socket(),false,sFileName,lFileSize,null,this.id_socketIn.getID_Socket());
     //this.jDialogRecepcion.show();
      cCopyImp.intCCopy.statusClusterConnection(new CCopyEvent(cCopyImp.CCOPY_STARTRECEIVE, sFileName, lFileSize, 0, this.id_socketIn.getID_Socket()));

     try
     {
      if(cCopyImp.getModo() == ClusterNet.MODE_DELAYED_RELIABLE
      || cCopyImp.getModo() == ClusterNet.MODE_RELIABLE)
      {
         //runGUI.getGUI().insertRecepcionString("Initializing reception of "+sFileName,null);
    	  cCopyImp.intCCopy.infoLog("Starting reception of file "+sFileName);

         lBytesLeidos = 0;
         byte[] bytes = new byte[1024*2];

         while(cCopyImp.esActiva()/* && (lBytesLeidos < lFileSize) */&& (this.file!=null))
         {

            //if(this.id_socketIn.available() > 0)
            //{
                //Log.log("Bytes disponibles: "+bytes.length,"");
                int iBytesLeidos = this.id_socketIn.read(bytes);

                //FIN DE FLUJO???...
                if(iBytesLeidos == -1)
                {
                  Log.log("FILERECPECION -> RECEIVEFILE : FIN DE FLUJO*","");
                  break;
                }

                //Ajustar tamaño...
                lBytesLeidos+= iBytesLeidos;
               // this.jDialogRecepcion.setBytesRecibidos(lBytesLeidos);
                cCopyImp.intCCopy.statusClusterConnection(new CCopyEvent(cCopyImp.CCOPY_STARTRECEIVE, sFileName, lFileSize, lBytesLeidos, this.id_socketIn.getID_Socket()));

                try
                {
                  this.fileOutputStream.write(bytes,0,iBytesLeidos);
                }
                catch(IOException e)
                {
                  mensajeErrorEscribiendo(e.getMessage());
                  throw e;
                }

            //}
            //else
            //  ClusterTimer.sleep(10);
         }

         //Mostrar resumen de recepción...
         long lTiempo = System.currentTimeMillis() - lTiempoInicio;
         this.resumenRecepcion(lTiempo,lBytesLeidos);
         
      }
      else
      {
          //NUNCA DEBE DE ENTRAR AQUI.
      }
  }
  finally
  {

     //Eliminar este objeto del treemap en la clase CCopyImp
     if(this.cCopyImp.getModo() == ClusterNet.MODE_DELAYED_RELIABLE
     || cCopyImp.getModo() == ClusterNet.MODE_RELIABLE)
     {
         this.cCopyImp.removeFileRecepcion(this.id_socketIn);
     }
     else
     {
        this.cCopyImp.removeFileRecepcion(this.id_socket);
     }

     //desactivar el diálogo de recepción...
     //if(this.jDialogRecepcion!= null)
     // this.jDialogRecepcion.setVisible(false);
     this.cCopyImp.intCCopy.statusClusterConnection(new CCopyEvent(CCopyImp.CCOPY_CANCEL,this.file.getName(),this.file.length(),-1,null));
     

     //Cerrar  Flujos...
     this.id_socketIn.close();
     this.id_socketIn = null;

     if(this.fileOutputStream!= null)
      this.fileOutputStream.close();


     if(cCopyImp.esActiva() && this.file!=null)
          {
             //Cambiar Localización y Nombre del fichero...
           File MFTPfile = new File(sFileName);
           if(MFTPfile.exists())
           {
             if(this.mensajeFileExists())
             {
              if (!MFTPfile.delete())
              {
                this.cCopyImp.intCCopy.errorLog("The file can't be deleted: "+sFileName+newline+"1. Check you hace privilegies."+newline+"2.Check the file is not used by another application.");
                this.cCopyImp.intCCopy.statusClusterConnection(new CCopyEvent(CCopyImp.CCOPY_CANCEL,this.file.getName(),this.file.length(),-1,null));
                
                //Eliminar temporal
                  this.file.delete();
                  this.file=null;
                return;
              }

               if(!file.renameTo(MFTPfile))
               {
                this.cCopyImp.intCCopy.errorLog("The file can't be renamed:"+sFileName+newline+"1. Check you hace privilegies."+newline+"2.Check the file is not used by another application.");
                this.cCopyImp.intCCopy.statusClusterConnection(new CCopyEvent(CCopyImp.CCOPY_CANCEL,this.file.getName(),this.file.length(),-1,null));
                
                //Eliminar temporal
                this.file.delete();
                  this.file=null;
               }
             }
           }
           else if(!file.renameTo(MFTPfile))
           {
             this.cCopyImp.intCCopy.errorLog("The file can't be renamed:"+sFileName+newline+"1. Check you hace privilegies."+newline+"2.Check the file is not used by another application.");
             this.cCopyImp.intCCopy.statusClusterConnection(new CCopyEvent(CCopyImp.CCOPY_CANCEL,this.file.getName(),this.file.length(),-1,null));
             
             //Eliminar temporal
              this.file.delete();
              this.file=null;
          }
    }




  }

 }

 //==========================================================================
 /**
  * Lee del flujo de entrada todos los bytes y los tira.
  */
  private void tirarBytes()throws IOException
  {
   // runGUI ftp = runGUI.getGUI();

    try
    {
         this.file = new File("nulo");
         
         this.cCopyImp.intCCopy.errorLog("Transfer just initiated. Waiting next transmission to join...");
         this.cCopyImp.intCCopy.statusClusterConnection(new CCopyEvent(CCopyImp.CCOPY_CANCEL,this.file.getName(),this.file.length(),-1,null));
         
         while(cCopyImp.esActiva() && (this.file!=null))
         {

                byte[] bytes = new byte[this.id_socketIn.available()];
                //Log.log("Bytes disponibles: "+bytes.length,"");
                int iBytesLeidos = this.id_socketIn.read(bytes);

                //FIN DE FLUJO???...
                if(iBytesLeidos == -1)
                {
                  Log.log("tirar bytes : FIN DE FLUJO**************************","");
                  return;
                }
         }
    }
    finally
    {
          this.file = null;
    }
  }


 //==========================================================================
 /**
  * Recibir UN FICHERO. NO Reliable Mode.
  */
 void receiveFile(long lFileSize, String sFileName)  throws IOException
 {

    this.lFileSize = lFileSize;
    this.sFileName = sFileName;
    Log.log("Fichero: "+sFileName+" longitud: "+lFileSize,"");
      //Fichero..
      this.file = new File(sFileName);

      //Comprobar si ya existe.
      if(this.file.exists())
      { if (mensajeFileExists())
        {
          if(!this.file.delete())
            throw new IOException("The file can't be deleted "+sFileName+newline+"1. Check you hace privilegies."+newline+"2.Check the file is not used by another application.");
        }
        else
        {
            throw new IOException("The file "+sFileName+" exist. Could not be overwrited.");
        }
      }

      //Crear el fichero...
      if(!this.file.createNewFile())
      {
          //mensajeErrorEscritura();
          throw new IOException("The file can't be created "+sFileName+newline+"1. Check you hace privilegies."+newline+"2.Check the file is not used by another application.");

      }

      //Comprobar si se puede escribir.
      if(!this.file.canWrite())
      {
          //mensajeErrorEscritura();
          throw new IOException("The file can't be writed "+sFileName+newline+"1. Check you hace privilegies."+newline+"2.Check the file is not used by another application.");
      }

      //Flujo de salida al fichero...
      this.fileOutputStream = new FileOutputStream(file);

     //Iniciar JDialogRecepcion
    // this.jDialogRecepcion = new JDialogRecepcion(this,null,"Receiving ",false,sFileName,lFileSize,null,this.id_socket);
    // this.jDialogRecepcion.show();
      cCopyImp.intCCopy.statusClusterConnection(new CCopyEvent(cCopyImp.CCOPY_STARTRECEIVE, sFileName, lFileSize, 0, this.id_socketIn.getID_Socket()));

      

   }

 //==========================================================================
 /**
  * Añadir Bytes. MODO NO FIABLE.
  */
  void addBytes(byte[] aBytes,int iBytes) throws IOException
  {

     if(this.file==null)
      return;

     if(lBytesLeidos < lFileSize)
     {
         //Ajustar tamaño...
         lBytesLeidos+= iBytes;
         //this.jDialogRecepcion.setBytesRecibidos(lBytesLeidos);
         cCopyImp.intCCopy.statusClusterConnection(new CCopyEvent(CCopyImp.CCOPY_STARTRECEIVE,this.file.getName(),this.file.length(),lBytesLeidos,null));
         
         try
         {
            this.fileOutputStream.write(aBytes,0,iBytes);
         }
         catch(IOException e)
         {
             mensajeErrorEscribiendo(e.getMessage());
             throw e;
         }
     }
     else
     {
       //if(this.jDialogRecepcion!= null)
       // this.jDialogRecepcion.setVisible(false);

       cCopyImp.intCCopy.statusClusterConnection(new CCopyEvent(CCopyImp.CCOPY_END,this.file.getName(),this.file.length(),lBytesLeidos,null));
    	    
       long lTiempo = System.currentTimeMillis() - lTiempoInicio;
       this.resumenRecepcion(lTiempo,lBytesLeidos);

       //Cerrar  Flujos
       if(this.fileOutputStream!= null)
         this.fileOutputStream.close();

      //Eliminar de CCopyImp
      this.cCopyImp.removeFileRecepcion(this.id_socket);

    }
 }


 //==========================================================================
 /**
  * Resumen recepcion
  */
 private void resumenRecepcion(long lTiempo,long lBytesTransmitidos)
 {
      long lHoras = 0;
      long lMinutos = 0;
      long lSegundos = 0;
     // long lTiempo = this.jDialogRecepcion.getlTiempo();
      String mensaje = "Transfer End. Received "+lBytesTransmitidos+" bytes in ";
    //  runGUI ftp = runGUI.getGUI();

      if (lTiempo > 1000)
      {
        //Calcular Horas
        lHoras = ((lTiempo/1000)/60)/60;
        lMinutos = ((lTiempo/1000)/60)%60;
        lSegundos = ((lTiempo/1000)%60);

        //Establecer el tiempo.....
        if(lHoras > 0)
          mensaje+=(lHoras+" hr. "+lMinutos+" min.");
        else if(lMinutos > 0)
          mensaje+=(lMinutos+" min. "+lSegundos+" seg.");
        else
          mensaje+=(lSegundos+" seg.");
      }
      else
          mensaje+=(lTiempo+" mseg.");



      //double dKB_seg = this.jDialogRecepcion.getdKB_seg();
      double dKB_seg ;
      dKB_seg = ((double)(lBytesTransmitidos)/(double)(lTiempo)) *1000;
      dKB_seg = (dKB_seg / 1024);
      
      //Comprobar que no es 0
      if (lTiempo == 0 && dKB_seg ==0)
          dKB_seg = lBytesTransmitidos;

      if (dKB_seg > 1)
      {
        int iParteEntera = (int)(dKB_seg );
        int iParteDecimal = (int)(dKB_seg *100)%100;
        //ftp.insertRecepcionString(mensaje+" Transfer rate: "+iParteEntera+"."+iParteDecimal+" KB/Seg","icono_tarea");
        cCopyImp.intCCopy.infoLog(mensaje+" Transfer rate "+iParteEntera+"."+iParteDecimal+" KB/Seg");
      }
      else
      {
        int i = (int)(dKB_seg * 100);
        //ftp.insertRecepcionString(mensaje+" Transfer rate: 0."+i+" KB/Seg","icono_tarea");
        cCopyImp.intCCopy.infoLog(mensaje+" Transfer rate: 0."+i+" KB/Seg");
      }

 }

 //==========================================================================
 /**
  * Recibir Identificador de runGUI ClusterNet v1.0
  * @return true si se ha recibido el Identificador de runGUI, false en caso contrario
  */
 private boolean receiveIDFTPMulticast() throws IOException
 {
   boolean bOK = false;
   try
   {
      Buffer buf = new Buffer(5);

      //Leer Datos...
      this.id_socketIn.read(buf.getBuffer());

      //Comprobar MAGIC
      if(buf.getInt(0) != 0x6DED757B)
        return bOK;

      //Comprobar VERSION
      if(buf.getByte(4) != 0x01)
        return bOK;

      bOK = true;

    Log.log("Iniciando la recepción runGUI...","");

   }
    finally{ return bOK;}
 }



 //==========================================================================
 /**
  * Recibir Nombre del Fichero
  * @return sFileName Nombre del Fichero
  */
 private String receiveFileName() throws IOException
 {
   String sFileName = null;
   byte[] bytes = null;
   try
   {
        Buffer buf = new Buffer(2);

        int iLong = 0;

        //Obtener la longitud del nombre del fichero...
        this.id_socketIn.read(buf.getBuffer());

        iLong = (int)buf.getShort(0);
        if(iLong > 0)
          bytes = new byte[iLong];
        this.id_socketIn.read(bytes);

        //Obtener el nombre del fichero...
        sFileName = new String(bytes);


        Log.log("Receiving file: "+sFileName+" from sender: "+this.id_socketIn.getID_Socket(),"");
   }

  finally{ return sFileName;}
 }

 //==========================================================================
 /**
  * Recibir Tamaño del Fichero
  * @return lSize Tamaño del Fichero
  */
 private long receiveFileSize() throws IOException
 {
   long lFileSize = 0;
   try
   {
        Buffer buf = new Buffer(8);

        //Obtener la longitud del nombre del fichero...
        this.id_socketIn.read(buf.getBuffer());

        lFileSize = buf.getLong(0);

        Log.log("Size: "+lFileSize+newline,"");
   }
   finally{ return lFileSize;}
 }

//==========================================================================
 /**
  * Recibir Nombre del Fichero
  * @param buf Un objeto Buffer con los datos.
  * @return sFileName Nombre del Fichero. null Si hay un error
  */
 public static String parseFileName(Buffer buf)
 {
   String sFileName = null;

   try
   {
        int iLong = 0;
        byte[] bytes = null;

        //Obtener la longitud del nombre del fichero...
        iLong = (int)buf.getInt(16);
        bytes = buf.getBytes(20,iLong);

        //Obtener el nombre del fichero...
        sFileName = new String(bytes);

       // this.getGUI().insertStringJTextPane(" ","icono_entrada");
       // this.getGUI().insertStringJTextPane("Recibiendo fichero: "+sFileName+newline,"entrada");
   }
   catch(ClusterNetInvalidParameterException e){;}

   finally{ return sFileName;}
 }

 //==========================================================================
 /**
  * Comprobar Tamaño del Fichero
  * @param buf Un objeto Buffer con los datos.
  * @return lSize Tamaño del Fichero. 0 si hay un error
  */
 public static long parseFileSize(Buffer buf) throws IOException
 {
   long lFileSize = 0;
   try
   {
      //Obtener la longitud  del fichero...
      lFileSize = buf.getLong(8);

     // this.getGUI().insertStringJTextPane(" ","icono_entrada");
     // this.getGUI().insertStringJTextPane("Tamaño: "+lFileSize+newline,"entrada");

   }
   catch(ClusterNetInvalidParameterException e){;}

   finally{ return lFileSize;}
 }

 //==========================================================================
 /**
  * Comprueba Identificador de CCopy
  * @param buf Un objeto Buffer con los datos leidos.
  * @return true si se ha recibido el Identificador de runGUI, false en caso contrario
  */
 public static boolean parseIDFTPMulticast(Buffer buf) throws IOException
 {
   boolean bOK = false;
   try
   {
      //Leer Magic
      if(buf.getInt(0) != CCopyImp.MAGIC)
       return bOK;

      if( buf.getInt(4) != CCopyImp.VERSION)
        return bOK;

      bOK = true;

   }
    finally{ return bOK;}
 }
 //==========================================================================
 /**
  * Recibir los bytes del fichero
  */
 private void receiveFileNO_Fiable(String sFileName)  throws IOException
 {
 /*    long lBytesLeidos = 0;

     try
     {
      // Crear Fichero.
      this.file = new File(sFileName);
      if(this.file.exists())
        if (mensajeFileExists())
        {
          if(!this.file.delete())
            return;
        }
        else
          return;

      //Crear el fichero...
      if(!this.file.createNewFile())
      {
        mensajeErrorEscritura();
        return;
      }

      if(!this.file.canWrite())
      {
        mensajeErrorEscritura();
        return;
      }

      //Flujo de salida al fichero...
      this.fileOutputStream = new FileOutputStream(file);
      if(this.modo == ClusterNet.MODE_DELAYED_RELIABLE
      || this.modo == ClusterNet.MODE_RELIABLE)
      {
         this.getGUI().insertStringJTextPane(" ","icono_entrada");
         this.getGUI().insertStringJTextPane("Iniciando la recepción... de "+sFileName+newline,"entrada");

         while(runFlag && lBytesLeidos < lFileSize)
         {
            if(this.id_socketIn.available() > 0)
            {
                byte[] bytes = new byte[this.id_socketIn.available()];
                int iBytesLeidos = this.id_socketIn.read(bytes);

                //Ajustar tamaño...
                lBytesLeidos+= iBytesLeidos;
                this.jDialogRecepcion.setBytesRecibidos(lBytesLeidos);
                try
                {
                  this.fileOutputStream.write(bytes,0,iBytesLeidos);
                }
                catch(IOException e)
                {
                  mensajeErrorEscribiendo(e.getMessage());
                  throw e;
                }

            }
         }
         this.fileOutputStream.close();

      }
      else
      {
      }
  }
  finally{;}
  */
 }


 //==========================================================================
 /**
  * Mensaje de advertencia--> Error Escribiendo
  */
 private void mensajeErrorEscribiendo(String sError)
 {
    JOptionPane.showMessageDialog(null,"Se ha producido un error mientras se intentaba escribir en el fichero"+newline+"\""+sFileName+newline+"\""+"El error es el siguiente:"+sError ,
				    "Error Escritura",JOptionPane.ERROR_MESSAGE);
 }


  //==========================================================================
 /**
  * Mensaje de advertencia--> El Fichero Existe. Petición de sobreescribir.
  * @return true si se quiere sobreescribir, false en caso contrario.
  */
 private boolean mensajeFileExists()
 {
  boolean b = false;
  try
  {
    int iOpcion =  JOptionPane.showConfirmDialog(null,"The file \""+sFileName+"\" exist."+newline+"¿Do you want overwrite it?",
				    "Overwrite", JOptionPane.YES_NO_OPTION);
    if(iOpcion == JOptionPane.YES_OPTION)
      b = true;


  }
  finally
  {
   return b;
  }
 }

 //==========================================================================
 /**
  * Parar transferencia
  */
 public void stopTransferencia()
 {
   // this.bStop = true;
   if(this.file!= null)
    this.file.delete();

   this.file = null;
   //runGUI.getGUI().insertRecepcionString("Reception canceled by the user.","icono_informacion");
   this.cCopyImp.intCCopy.errorLog("Reception canceled by the user!.");
   this.cCopyImp.intCCopy.statusClusterConnection(new CCopyEvent(CCopyImp.CCOPY_CANCEL,this.file.getName(),this.file.length(),-1,null));
  
 }
 }
