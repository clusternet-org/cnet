//============================================================================
//
//	Copyright (c) 1999 . All Rights Reserved.
//
//----------------------------------------------------------------------------
//
//	Fichero: PruebaSocketPTMFRecepcion.java  1.0 10/11/99
//
// 	Autores: 	M. Alejandro García Domínguez (alejandro.garcia.dominguez@gmail.com)
//			    Antonio Berrocal Piris
//
//	Descripción: PruebaSocketPTMFRecepcion
//
//----------------------------------------------------------------------------

package org.clusternet.test;

import java.lang.*;
import java.util.*;

import org.clusternet.Address;
import org.clusternet.Buffer;
import org.clusternet.ID_Socket;
import org.clusternet.ID_SocketInputStream;
import org.clusternet.Log;
import org.clusternet.ClusterNetInputStream;
import org.clusternet.ClusterNetOutputStream;
import org.clusternet.ClusterNet;
import org.clusternet.ClusterNetConnectionListener;
import org.clusternet.ClusterNetErrorListener;
import org.clusternet.ClusterNetEventConecction;
import org.clusternet.ClusterNetEventError;
import org.clusternet.ClusterNetEventGroup;
import org.clusternet.ClusterNetEventMember;
import org.clusternet.ClusterNetEventMemberInputStream;
import org.clusternet.ClusterNetExcepcion;
import org.clusternet.ClusterNetGroupListener;
import org.clusternet.ClusterNetMemberInputStreamListener;
import org.clusternet.ClusterNetMemberListener;
import org.clusternet.ClusterNetInvalidParameterException;
import org.clusternet.ClusterNet;
import org.clusternet.ClusterTimer;

import java.net.*;
import java.io.*;

/**
 * Clase de prueba de un FTP Receptor en modo consola.
 * @author M. Alejandro García Domínguez
 * <A HREF="mailto:alejandro.garcia.dominguez@gmail.com">(alejandro.garcia.dominguez@gmail.com)</A><p>
 *			   Antonio Berrocal Piris
 */
public class FTPReceptor implements                ClusterNetConnectionListener,
                                                   ClusterNetErrorListener,
                                                   ClusterNetGroupListener,
                                                   ClusterNetMemberListener,
                                                  ClusterNetMemberInputStreamListener
{
 /**El socket ClusterNet*/
 private ClusterNet mSocket = null;

 /**Direccion Multicast*/
 private Address dirMcast = null;

 /**Flujo de salida Multicast*/
 private ClusterNetOutputStream mOut = null;

 /** Objeto File */
 private File file = null;

 /** Objeto FileOutputStream */
 private FileOutputStream fileOutputStream = null;

 /** Tamaño del Fichero */
 private long lFileSize = 0;

 /** Nombre del Fichero */
 private String sFileName = null;

 /** Tiempo Inicial */
 private long lTiempoInicial = 0;

 /** Numero de bytes leidos*/
 long lBytesLeidos = 0;

 /**  ID_SocketInputStream */
 private ID_SocketInputStream id_socketIn = null;

 /** ID_Socket */
 private ID_Socket id_socket = null;

 /**Flujo de entrada Multiast*/
 private ClusterNetInputStream mIn = null;

 /** Fin del proceso */
 private boolean bFin = false;

 //==========================================================================
 /**
  * Constructor.
  */
 public FTPReceptor()
 {
    super();
 }

 //==========================================================================
 /**
  *  Cuerpo de la clase.
  */
 public void run()
 {
  final String mn  = "PruebaSocket.run()";

  try
  {

    // Establecer Dirección Multicast y Puerto Multicast
    dirMcast = new Address (InetAddress.getByName ("224.2.2.2"),20);

    Log.log("","Creando el ClusterNet....");


    //Crear el socket ClusterNet a la direccion Multicast anterior y con TTL = 2
    //-El socket se crea en Modo MODE_RELIABLE
    //-se registra el listener ClusterNetConnectionListener.
    //-Sin seguridad
    mSocket = new ClusterNet(dirMcast,null,(byte)2,ClusterNet.MODE_RELIABLE,this,null,null);

    Log.log("","ClusterNet creado...");

    //Registrar Listeners...
    mSocket.addErrorListener(this);
    mSocket.addMemberListener(this);
    mSocket.addGroupListener(this);


    //Obtener los flujos de entrada y salida...
    mOut = mSocket.getClusterOutputStream();
    mIn  = mSocket.getClusterInputStream();

    //Registra listener en flujo de entrada...
    mIn.addPTMFID_SocketInputStreamListener(this);

    //MODO DE LECTURA ASINCRONA
    //Leer todo lo que le hechen...
    Log.log("","Esperar llegada de datos...");

    while(!bFin)
    {
        ClusterTimer.sleep(500);
    }

    //recibir fichero
    receiveFile();

   }
   catch (ClusterNetInvalidParameterException pie)
   {
       Log.log (mn,"Parámetro inválido: " + pie.toString());
       Log.exit (1);
   }
   catch (ClusterNetExcepcion e)
   {
       Log.log (mn,"ClusterNet Excepción: " + e.toString ());
       Log.exit (1);
   }
   catch (UnknownHostException e)
   {
       Log.log (mn,"Host desconocido. " + e.toString ());
       Log.exit (1);
   }
   catch (IOException e)
   {
       Log.log (mn,e.toString ());
       Log.exit (1);
   }
  }
//==========================================================================
 /**
  * Recibir UN FICHERO
  */
  private void receiveFile()  throws IOException
 {
     this.file = null;
      lTiempoInicial= System.currentTimeMillis();
     //Log.log("receiveFile()","");

     //---------------------------------
     //Recibir ID...
     if(!receiveIDFTPMulticast())
     {
        //tirarBytes();
        return;
     }

     //Recibir FileSize...
     lFileSize = this.receiveFileSize();
     if(lFileSize <= 0)
     {
        //tirarBytes();
        return;
     }

     //Recibir FileName...
     sFileName = this.receiveFileName();
     if (sFileName == null)
     {
        //tirarBytes();
        return;
     }

      //Fichero temporal..
      this.file = File.createTempFile("org.clusternet.ftp"+System.currentTimeMillis(),".tmp");

      //Comprobar si ya existe.
      if(this.file.exists())
      {
        if(!this.file.delete())
            throw new IOException("No se ha podido borrar el fichero "+sFileName+"1. Compruebe si tiene privilegios."+"2.Compruebe que el fichero no está siendo usado por otra aplicación.");

      }

      //Crear el fichero...
      if(!this.file.createNewFile())
      {
          //mensajeErrorEscritura();
          throw new IOException("No se ha podido CREAR el fichero "+sFileName+"1. Compruebe si tiene privilegios."+"2.Compruebe que el fichero no está siendo usado por otra aplicación.");

      }

      //Comprobar si se puede escribir.
      if(!this.file.canWrite())
      {
          //mensajeErrorEscritura();
          throw new IOException("No se puede ESCRIBIR en el fichero "+sFileName+"1. Compruebe si tiene privilegios."+"2.Compruebe que el fichero no está siendo usado por otra aplicación.");
      }

      //Flujo de salida al fichero...
      this.fileOutputStream = new FileOutputStream(file);

     try
     {
         lBytesLeidos = 0;
         byte[] bytes = new byte[ClusterNet.MTU];

         while((lBytesLeidos < lFileSize) && (this.file!=null))
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
                try
                {
                  this.fileOutputStream.write(bytes,0,iBytesLeidos);
                }
                catch(IOException e)
                {
                  mensajeErrorEscribiendo(e.getMessage());
                  throw e;
                }
         }//while

         //Mostrar resumen de recepción...
         this.resumenRecepcion();


  }
  finally
  {


     //Cerrar  Flujos...
     this.id_socketIn.close();
     this.id_socketIn = null;

     if(this.fileOutputStream!= null)
      this.fileOutputStream.close();


     if( this.file!=null)
          {
             //Cambiar Localización y Nombre del fichero...
           File MFTPfile = new File(sFileName);
           if(MFTPfile.exists())
           {
             if(this.mensajeFileExists())
             {
              if (!MFTPfile.delete())
              {
                Log.log("No se ha podido eliminar el fichero: "+sFileName,"1. Compruebe si tiene privilegios suficientes. 2.Compruebe que el fichero no está siendo usado por otra aplicación.");
                 //Eliminar temporal
                  this.file.delete();
                  this.file=null;
                return;
              }

               if(!file.renameTo(MFTPfile))
               {
                Log.log("No se ha podido renombrar a:"+sFileName,"1. Compruebe si tiene privilegios suficientes. 2.Compruebe que el fichero no está siendo usado por otra aplicación.");
                //Eliminar temporal
                this.file.delete();
                  this.file=null;
               }
             }
           }
           else if(!file.renameTo(MFTPfile))
           {
             Log.log("No se ha podido renombrar a:"+sFileName,"1. Compruebe si tiene privilegios suficientes. 2.Compruebe que el fichero no está siendo usado por otra aplicación.");
              //Eliminar temporal
              this.file.delete();
              this.file=null;
          }
    }




  }

 }

/**
  * IMplementacion interfaz PTMFID_SocketInpUtStream
  */
  public void actionPTMFID_SocketInputStream(ClusterNetEventMemberInputStream evento)
  {
    Log.log("Nuecvo ID_Socket InputStream","");
    bFin = true;
    this.id_socketIn = evento.getID_SocketInputStream();
    this.id_socket = id_socketIn.getID_Socket();
  }


 //==========================================================================
 /**
  * IMplementacion interfaz ClusterNetConnectionListener
  */
  public void actionNewConnection(ClusterNetEventConecction evento)
  {
    Log.log("Conexion: ",evento.getString());
  }

 //==========================================================================
 /**
  * IMplementacion interfaz ClusterNetErrorListener
  */
  public void actionError(ClusterNetEventError evento)
  {
    Log.log("Error: ",evento.getString());
  }

 //==========================================================================
 /**
  * IMplementacion interfaz ClusterNetGroupListener
  */
  public void actionPTMFIDGL(ClusterNetEventGroup evento)
  {
    if(evento.esAñadido())
      Log.log("Nuevo IDGL: " + evento.getIDGL(),"");
    else
      Log.log("IDGL eliminado: " + evento.getIDGL(),"");
  }

 //==========================================================================
 /**
  * IMplementacion interfaz ClusterNetMemberListener
  */
  public void actionID_Socket(ClusterNetEventMember evento)
  {
    if(evento.esAñadido())
      Log.log("Nuevo ID_Socket: " + evento.getID_Socket(),"");
    else
      Log.log("ID_Socket eliminado: " + evento.getID_Socket(),"");
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

        Log.log("Recibiendo fichero: "+sFileName+" del emisor: "+this.id_socketIn.getID_Socket(),"");
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


        Log.log("Tamaño: "+lFileSize,"");
   }
   finally{ return lFileSize;}
 }
//==========================================================================
 /**
  * Recibir Identificador de cFtp ClusterNet v1.0
  * @return true si se ha recibido el Identificador de cFtp, false en caso contrario
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

      Log.log("Iniciando la recepción cFtp...","");

   }
    finally{ return bOK;}
 }


//==========================================================================
 /**
  * Mensaje de advertencia--> El Fichero Existe. Petición de sobreescribir.
  * @return true si se quiere sobreescribir, false en caso contrario.
  */
 private boolean mensajeFileExists()
 {
  boolean b = true;
  try
  {
    Log.log("El fichero "+sFileName+"ya existe. ¿Desea sobreescribir el fichero existente?",
				    "Sobreescribir");

  }
  finally
  {
   return b;
  }
 }

 //==========================================================================
 /**
  * Mensaje de advertencia--> No se puede escribir en el fichero.
  */
 private void mensajeErrorEscritura()
 {
    Log.log("No se puede escribir en el fichero: "+sFileName+"no se tiene permiso de escritura"+"Verifique los permisos de escritura y que tiene suficiente privilegio para escribir." ,
				    "Error Escritura");
 }

 //==========================================================================
 /**
  * Mensaje de advertencia--> Error Escribiendo
  */
 private void mensajeErrorEscribiendo(String sError)
 {
   Log.log("Se ha producido un error mientras se intentaba escribir en el fichero"+sFileName+"El error es el siguiente:"+sError ,
				    "Error Escritura");
 }
//==========================================================================
 /**
  * Resumen recepcion
  */
 private void resumenRecepcion()
 {
      long lHoras = 0;
      long lMinutos = 0;
      long lSegundos = 0;
      long lTiempo = System.currentTimeMillis()- lTiempoInicial ;
      String mensaje = "Transferencia Finalizada. Recibido "+lBytesLeidos+" bytes en ";

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

      double dKB_seg =  ((double)(lBytesLeidos * 1000)/(double)(lTiempo));
    dKB_seg = (dKB_seg / 1024);



      if (dKB_seg > 1)
      {
        int iParteEntera = (int)(dKB_seg );
        int iParteDecimal = (int)(dKB_seg *100)%100;
        Log.log(mensaje+" Ratio Transferencia: "+iParteEntera+"."+iParteDecimal+" KB/Seg","informacion");
      }
      else
      {
        int i = (int)(dKB_seg * 100);
        Log.log(mensaje+" Ratio Transferencia: 0."+i+" KB/Seg","informacion");
      }

 }


 //==========================================================================
 /**
  * Metodo main()
  */
  public static void main (String args[])
  {
       new FTPReceptor().run();
  }
}



