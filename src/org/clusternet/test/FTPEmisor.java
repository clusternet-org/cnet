//============================================================================
//
//	Copyright (c) 1999,2014 . All Rights Reserved.
//
//----------------------------------------------------------------------------
//
//	Fichero: PruebaSocketPTMFEmision.java  1.0 10/11/99
//
//	Descripción: PruebaSocketPTMFEmision
//
//  Historial: 
//	14/10/2014 Change Licence to LGPL
//
// 	Authors: 
//		 Alejandro Garcia Dominguez (alejandro.garcia.dominguez@gmail.com)
//		 Antonio Berrocal Piris (antonioberrocalpiris@gmail.com)
//
//
//      This file is part of ClusterNet 
//
//      ClusterNet is free software: you can redistribute it and/or modify
//      it under the terms of the Lesser GNU General Public License as published by
//      the Free Software Foundation, either version 3 of the License, or
//      (at your option) any later version.
//
//      ClusterNet is distributed in the hope that it will be useful,
//      but WITHOUT ANY WARRANTY; without even the implied warranty of
//      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//      Lesser GNU General Public License for more details.
//
//      You should have received a copy of the Lesser GNU General Public License
//      along with ClusterNet.  If not, see <http://www.gnu.org/licenses/>.

//----------------------------------------------------------------------------


package org.clusternet.test;

import java.lang.*;
import java.util.*;

import org.clusternet.Address;
import org.clusternet.Buffer;
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
import org.clusternet.ClusterNetExcepcion;
import org.clusternet.ClusterNetGroupListener;
import org.clusternet.ClusterNetMemberListener;
import org.clusternet.ClusterNetInvalidParameterException;
import org.clusternet.ClusterNet;
import org.clusternet.ClusterTimer;

import java.net.*;
import java.io.*;


/**
 * Clase de prueba de un FTPEmisor en modo consola.
 * @author M. Alejandro García Domínguez
 * <A HREF="mailto:alejandro.garcia.dominguez@gmail.com">(alejandro.garcia.dominguez@gmail.com)</A><p>
 *			   Antonio Berrocal Piris
 */
public class FTPEmisor implements
      ClusterNetConnectionListener, ClusterNetErrorListener,  ClusterNetGroupListener,
      ClusterNetMemberListener

{
 /** Numero de mensajes a enviar*/
 static final int NUM_DATOS = 120;

 /**El socket ClusterNet*/
 private ClusterNet mSocket = null;

 /**Direccion Multicast*/
 private Address dirMcast = null;

 /**Flujo de salida Multicast*/
 private ClusterNetOutputStream mOut = null;

 /**Flujo de entrada Multiast*/
 private ClusterNetInputStream mIn = null;

 /** Objeto File*/
 private File file = null;

 /** Objeto FileInputStream */
 private FileInputStream fileInputStream = null;


 //==========================================================================
 /**
  * Constructor.
  */
 public FTPEmisor(String sFileName)
 {
    super();

    try
    {
    file = new  File(sFileName);
    fileInputStream = new FileInputStream(file);
    }catch(FileNotFoundException e){ Log.log(""+e,"");}
 }

 //==========================================================================
 /**
  *  Cuerpo de la clase.
  */
 public void run()
 {
  final String mn  = "PruebaSocketEmision.run()";
  long tiempoInicio = 0;
  long bytesEnviados = 0;

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
    //mSocket.addPTMFConexionListener(this);  <<-- No es necesario, ya se ha
    //                                             registrado en el constructor
    mSocket.addErrorListener(this);
    mSocket.addMemberListener(this);
    mSocket.addGroupListener(this);


    //Obtener los flujos de entrada y salida...
    mOut = mSocket.getClusterOutputStream();
    mIn  = mSocket.getClusterInputStream();

    //******** MUY  IMPORTANTE: ***********
    //* Esta clase no lee datos de la red.*
    //*************************************
    // * ---> DESACTIVAR RECEPCION  <--- *
    mSocket.disableRx();

    //Esperar a que CGL reciba todos los TPDUs GRUPO_LOCAL_VECINO de todos los IDGLs vecinos
    ClusterTimer.sleep(2000);

    //Enviar el fichero...
    sendFile();

    ClusterTimer.sleep(2000);

    Log.log("","Cerrando el socket....");

    //Cerrar la conexion Multicast de forma estable....
    mSocket.close(ClusterNet.CLOSE_STABLE/*ESTABLE*/);
    Log.log("","Socket cerrado");


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
   * Enviar Fichero
   */
   void sendFile() throws IOException
   {
     try
     {
           //Información del fichero....
          Log.log("Iniciando transferencia cFtp....","");
          Log.log("Tamaño del fichero: "+this.file.length()+" bytes","");

          //Enviar IDFTP, Tamaño y Nombre del Fichero.....
          this.sendCabeceraFTP(this.file.length(),this.file.getName());

          //Buffer
          byte[] aBytes =  new byte[ClusterNet.MTU];

          Log.log("Timepo inicial....","");
          long lTiempoInicio = System.currentTimeMillis();
          long lBytesTransmitidos = 0;
          long lFile = this.file.length();

          //Transferir el FICHERO....
          for(lBytesTransmitidos = 0; lBytesTransmitidos<lFile ;)
          {
              //Leer bytes...
              int iBytesLeidos = this.fileInputStream.read(aBytes);

              if(iBytesLeidos == -1)
                break; //FIN FLUJO....

              //Log.log("\n\nBYTES LEIDOS: "+iBytesLeidos,"");

              //Transmitir los bytes leidos...
              this.sendBytes(aBytes,iBytesLeidos);

              //Ajustar bytes transmitidos..
              lBytesTransmitidos+= iBytesLeidos;



          }
          long lTiempo = System.currentTimeMillis() - lTiempoInicio;
          long lHoras = 0;
          long lMinutos = 0;
          long lSegundos = 0;

          String mensaje = "Transmitido "+lBytesTransmitidos+" bytes en ";

            //Calcular Horas
            lHoras = ((lTiempo/1000)/60)/60;
            lMinutos = ((lTiempo/1000)/60)%60;
            lSegundos = ((lTiempo/1000)%60);

         Log.log(mensaje+lHoras+":"+lMinutos+":"+lSegundos,"");


         //Emisión Fichero Finalizada....
         //resumenTransferencia();
         this.file = null;
        // this.bStop = true;
       }
       finally
       {
          try
          {
            //Cerrar Flujo Multicast...
            if(mOut!=null)
             this.mOut.close();

            //Cerrar flujo fichero...
            if(this.fileInputStream!= null)
                this.fileInputStream.close();
          }
          catch(IOException ioe){;}

       }
   }




   //==========================================================================
 /**
  * Enviar un array de bytes
  * @param aBytes Un array de bytes
  * @param iBytes Número de Bytes dentro del array a transmitir.
  */
 private void sendBytes(byte[] aBytes,int iBytes) throws IOException
 {
      this.mOut.write(aBytes,0,iBytes);
 }


 //==========================================================================
 /**
  * Enviar Identificador de cFtp ClusterNet v1.0, Enviar Tamaño del Fichero,
  * Enviar Nombre del Fichero.....
  */
 private void sendCabeceraFTP(long lSize,String sFileName) throws IOException
 {
      Buffer buf = new Buffer(15 + sFileName.length());

     //ID_FTP
      buf.addInt(0x6DED757B,0);
      buf.addByte((byte)0x01,4);

      //Tamaño.-
      buf.addLong(lSize,5);
      Log.log("Enviando tamaño: "+lSize,"");

      //Nombre del Fichero.-
      buf.addShort(sFileName.length(),13);
      buf.addBytes(new Buffer(sFileName.getBytes()),0,15,sFileName.length());

      Log.log("Enviando nombre del fichero: "+sFileName,"");

      this.mOut.write(buf.getBuffer());
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
      Log.log("Nuevo ClusterGroupID: " + evento.getIDGL(),"");
    else
      Log.log("ClusterGroupID eliminado: " + evento.getIDGL(),"");
  }

 //==========================================================================
 /**
  * IMplementacion interfaz ClusterNetMemberListener
  */
  public void actionID_Socket(ClusterNetEventMember evento)
  {
    if(evento.esAñadido())
      Log.log("Nuevo ClusterMemberID: " + evento.getID_Socket(),"");
    else
      Log.log("ClusterMemberID eliminado: " + evento.getID_Socket(),"");
  }


 //==========================================================================
 /**
  * Metodo main()
  */
  public static void main (String args[])
  {
       new FTPEmisor(args[0]).run();
  }
}




