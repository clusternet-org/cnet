//============================================================================
//
//	Copyright (c)2016 clusternet.org - All Rights Reserved.
//
//----------------------------------------------------------------------------
//
//	Description: FileEmision
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
  * Class FileEmision.
  * Data relative to the file to be transmitted using CCopy / clusterNet
  */
  class FileEmision
  {
   /** Objeto File */
   private File file = null;

   /** Objeto FileInputStream */
   private FileInputStream fileInputStream = null;
  
   /** CCopyImp */
   private CCopyImp cCopyImp = null;

   /** Nueva Línea */
   private static final String newline = "\n";

   /** Icon */
   private Icon icon = null;

   /** Flag para para la transferencia */
   private boolean bStop = false;

   /** Flujo de Salida Multicast */
   private ClusterNetOutputStream out = null;

   /** Interface for callbacks */
   private InterfaceCCopy intCCopy;

  //==========================================================================
  /**
   * Constructor.
 * @param intCCopy 
   */
   public  FileEmision(CCopyImp cCopyImp,File file, InterfaceCCopy intCCopy) throws IOException
   {
      this.file = file;
      this.cCopyImp = cCopyImp;
      this.icon = icon;
      this.intCCopy = intCCopy;

      if(this.cCopyImp.getModo() == ClusterNet.MODE_DELAYED_RELIABLE
        || this.cCopyImp.getModo() == ClusterNet.MODE_RELIABLE )
      {
        //Flujo de salida Multicast ....
        out = this.cCopyImp.getSocket().getClusterOutputStream();
        if(out == null)
        {
          throw new IOException("Multicast sender flow is NULL.\n");
        }
      }

      if(file == null)
      {
        throw new IOException("File name is NULL.\n");
      }


      if(!this.file.exists())
      {
        throw new IOException("The system is not locating "+this.file.getName()+" \n as a valid file");
      }

      if(!this.file.canRead())
      {
        throw new IOException("The file "+this.file.getName()+" \n can't be reader.\nCheck read permission");
      }


      //Crear el Flujo de lectura del Fichero....
      fileInputStream = new FileInputStream(this.file);

      //Flujo de Salida Multicast...
      //this.out = this.protocoloFTPMulticast.getMulticastOutputStream();

     
  }


  //==========================================================================
  /**
   * Enviar Fichero
   */
   void sendFile() throws IOException
   {
     //runGUI ftp = runGUI.getGUI();

     try
     {
          //Información del fichero....
    	  intCCopy.infoLog("Sending file to cluster members -> "+this.file.getName());
    	  intCCopy.infoLog("File size: "+this.file.length()+" bytes");

          if(!this.cCopyImp.esActiva())
            return;

          intCCopy.statusClusterConnection(new CCopyEvent(CCopyImp.CCOPY_STARTSEND,this.file.getName(),this.file.length(),0,null));
          
        

          //Enviar IDFTP, Tamaño y Nombre del Fichero.....
          this.sendCabeceraFTP(this.file.length(),this.file.getName());

          //Buffer
          byte[] aBytes =  new byte[CCopyImp.TAMAÑO_ARRAY_BYTES];

          long lTiempoInicio = System.currentTimeMillis();
          long lBytesTransmitidos = 0;
          long lFile = this.file.length();

          if(!this.cCopyImp.esActiva())
            return;

          //Transferir el FICHERO....
          for(lBytesTransmitidos = 0; lBytesTransmitidos<lFile && this.cCopyImp.esActiva();)
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
              //this.jDialogTransferencia.setBytesTransmitidos(lBytesTransmitidos);
              intCCopy.statusClusterConnection(new CCopyEvent(CCopyImp.CCOPY_STARTSEND,this.file.getName(),this.file.length(),lBytesTransmitidos,null));
              

              if(this.bStop == true || !this.cCopyImp.esActiva())
              {
                 file = null;

                 intCCopy.statusClusterConnection(new CCopyEvent(CCopyImp.CCOPY_CANCEL,this.file.getName(),this.file.length(),lBytesTransmitidos,null));
                

                 intCCopy.infoLog("Transfer cancelled by user!.");
                 return;
              }
          }
          long lTiempo = System.currentTimeMillis() - lTiempoInicio;
          long lHoras = 0;
          long lMinutos = 0;
          long lSegundos = 0;

          String mensaje = "Transmited "+lBytesTransmitidos+" bytes in ";

            //Calcular Horas
            lHoras = ((lTiempo/1000)/60)/60;
            lMinutos = ((lTiempo/1000)/60)%60;
            lSegundos = ((lTiempo/1000)%60);

         Log.log(mensaje+lHoras+":"+lMinutos+":"+lSegundos,"");


       

         //Emisión Fichero Finalizada....
         resumenTransferencia(lTiempo,lBytesTransmitidos);
         this.file = null;
         this.bStop = true;
       }
       finally
       {
          try
          {
            //Cerrar Flujo Multicast...
            if(out!=null)
              this.out.close();

            //Cerrar flujo fichero...
            if(this.fileInputStream!= null)
                this.fileInputStream.close();
          }
          catch(IOException ioe){;}

       }
   }

 //==========================================================================
 /**
  * Parar transferencia
  */
 public void stopTransferencia()
 {
   this.bStop = true;
   this.file = null;

   this.cCopyImp.file = null;
 }


 //==========================================================================
 /**
  * Resumen
  */
 private void resumenTransferencia(long lTiempo,long lBytesTransmitidos)
 {
      long lHoras = 0;
      long lMinutos = 0;
      long lSegundos = 0;
      long lMSegundos = 0;
      String mensaje = "Transmited "+lBytesTransmitidos+" bytes in ";
     // runGUI ftp = runGUI.getGUI();

      if (lTiempo > 1000)
      {
        //Calcular Horas
        lHoras = ((lTiempo/1000)/60)/60;
        lMinutos = ((lTiempo/1000)/60)%60;
        lSegundos = ((lTiempo/1000)%60);
        lMSegundos = (lTiempo%1000);

        //Establecer el tiempo.....
        if(lHoras > 0)
          mensaje+=(lHoras+" hr. "+lMinutos+" min.");
        else if(lMinutos > 0)
          mensaje+=(lMinutos+" min. "+lSegundos+" seg.");
        else
          mensaje+=(lSegundos+" seg."+lMSegundos+" mseg.");
      }
      else
          mensaje+=(lTiempo+" mseg.");



      double dKB_seg ;
      dKB_seg = ((double)(lBytesTransmitidos)/(double)(lTiempo)) *1000;
      dKB_seg = (dKB_seg / 1024);

      if (dKB_seg > 1)
      {
        int iParteEntera = (int)(dKB_seg );
        int iParteDecimal = (int)(dKB_seg *100)%100;
        intCCopy.infoLog(mensaje+" Transfer rate "+iParteEntera+"."+iParteDecimal+" KB/Seg");
      }
      else
      {
        int i = (int)(dKB_seg * 100);
        intCCopy.infoLog(mensaje+" Transfer rate: 0."+i+" KB/Seg");
      }
      intCCopy.statusClusterConnection(new CCopyEvent(CCopyImp.CCOPY_END,this.file.getName(),this.file.length(),lBytesTransmitidos,null));
      

  }





   //==========================================================================
 /**
  * Enviar un array de bytes
  * @param aBytes Un array de bytes
  * @param iBytes Número de Bytes dentro del array a transmitir.
  */
 private void sendBytes(byte[] aBytes,int iBytes) throws IOException
 {
    if(this.cCopyImp.getModo() == ClusterNet.MODE_DELAYED_RELIABLE
    || this.cCopyImp.getModo() == ClusterNet.MODE_RELIABLE)
    {
      this.out.write(aBytes,0,iBytes);
    }
    else
    {
      Buffer buf = new Buffer(aBytes);
      buf.setLength(iBytes);
      this.cCopyImp.getDatagramSocket().send(buf);
    }
 }


 //==========================================================================
 /**
  * Enviar Identificador de runGUI ClusterNet v1.0, Enviar Tamaño del Fichero,
  * Enviar Nombre del Fichero.....
  */
 private void sendCabeceraFTP(long lSize,String sFileName) throws IOException
 {
     
      Buffer buf = new Buffer(15 + sFileName.length());

     //ID_FTP
      buf.addInt(CCopyImp.MAGIC,0);
      buf.addByte((byte)CCopyImp.VERSION,4);

      //Tamaño.-
      buf.addLong(lSize,5);
     // ftp.insertTransmisionString("Sending size: "+lSize,null);

      //Nombre del Fichero.-
      buf.addShort(sFileName.length(),13);
      buf.addBytes(new Buffer(sFileName.getBytes()),0,15,sFileName.length());

      //ftp.insertTransmisionString("Sending file name: "+sFileName,null);

    if(this.cCopyImp.getModo() == ClusterNet.MODE_DELAYED_RELIABLE
    || this.cCopyImp.getModo() == ClusterNet.MODE_RELIABLE)
    {
     //ENVIAR BUFFER Y STRING...
      this.out.write(buf.getBuffer());
    }
    else
    {
      //ENVIAR LOS DATOS.....
      this.cCopyImp.getDatagramSocket().send(buf);
    }

 }

 }
