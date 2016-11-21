//============================================================================
//
//	Copyright (c) 2016 . All Rights Reserved.
//
//----------------------------------------------------------------------------
//
//	Fichero: mping.java  1.0 19/11/2016
//
//	Description: mPIng      
//
// 	Authors: 
//		 Alejandro García-Domínguez (alejandro.garcia.dominguez@gmail.com)
//
//
//  Historial: 
//	19.11.2016 Initial release
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//
//----------------------------------------------------------------------------
package cnet.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Date;

import cnet.Address;
import cnet.Buffer;
import cnet.Log;

/**
 * <p>Title: mping</p>
 *
 * <p>Description: Test de red multicast</p>
 * Utilidad para enviar y recibir paquetes de sondeo multicast
 * <p>Copyright: Copyright (c) 2016 Alejandro Garcia</p>
 *
 * <p>Company: </p>
 *
 * @author Alejandro Garcia
 * @version 1.0
 */
public class mping {

	

/** Tamaño del array de Transmisión/Recepcion */
//public static final int TAMAÑO_ARRAY_BYTES = 1024 * 2;
private int payload_size = 32;

/** Nueva Línea */
private static final String newline = "\n";

/** Dirección Multicast:puerto*/
private Address dirIPMcast = null;

/**TTL sesion */
private int TTLSesion = 8;


/** Socket Multicast Fiable*/
private MulticastSocket socket = null;

/** Flag de inicio del thread */
private boolean runFlag = true;

/** Ratio ed transferencia */
private long lRatio = 0;

private int buffer_size;


//==========================================================================
/**
* Constructor
 * @param ttl 
 * @param args2 
 * @param payload 
*/
public mping(String dirIPMulticast, String puerto, String ttl, String payload_size) throws IOException
{
//System.out.println("");
//System.out.println("");
banner();


//Obtener parámetros
this.dirIPMcast = new Address(dirIPMulticast,Integer.parseInt(puerto));
this.TTLSesion = Integer.parseInt(ttl);

this.payload_size=Integer.parseInt(payload_size);
this.buffer_size = this.payload_size+4+4+8+4;

}


private static void banner()
{
	System.out.println("---------------------------------------------------------------------");
	System.out.println("clusterNet mping v1.0");
	System.out.println("(c) 2016 M.Alejandro Garcia");
	System.out.println("---------------------------------------------------------------------");
	System.out.println("Warning: Clock's hosts must been synchronized by NTP to work propertly");
	System.out.println("---------------------------------------------------------------------");
}

//==========================================================================
/**
* Método Run()
*/
public void run()
{


try
{


    Date today;
    String output;
    SimpleDateFormat formatter;

    formatter = new SimpleDateFormat("yyyy.MM.dd '/' HH:mm:ss");
    today = new Date();
    output = formatter.format(today);
    System.out.println(output);



    //Crear el socket multicast
    socket = new MulticastSocket(dirIPMcast.getPort());
    Log.log("Socket Multicast"," OK");

    //Join
    socket.joinGroup(dirIPMcast.getInetAddress());
    Log.log("Join"," "+dirIPMcast.getHostAddress()+":"+dirIPMcast.getPort());

    //Buffer de recepcion
    
    
    socket.setReceiveBufferSize(1024*64);

    //SoTimeout
    socket.setSoTimeout(100);
    Log.log("SoTimeout",""+socket.getSoTimeout());
    
    socket.setTimeToLive(this.TTLSesion);
    Log.log("TTL"," "+this.TTLSesion);
    
    NetworkInterface ni = socket.getNetworkInterface();
    Log.log("Multicast interface"," "+ni);
    

     //byte[] buf = new byte[this.TAMAÑO_ARRAY_BYTES];
     Buffer bufSend = new Buffer(this.buffer_size);
     Buffer bufRecv = new Buffer(this.buffer_size);
     
     DatagramPacket recv = new DatagramPacket(bufRecv.getBuffer(), bufRecv.getMaxLength());
     

     int sizeRcv = 0;
     int contador = 0;
     int recvPuerto =0;
     String sMensaje = null;
     String sContador = null;
     String[] sTokens = null;

     int secuencia = 0;
     long sendTime = 0;  
     
     //Nuevo payload
     Buffer payloadSend = new Buffer(this.payload_size);
     
     for (int i=0; i<payload_size;i=i+4)
     {
    	 payloadSend.addInt(i);
     }
    	 
    	 
     
    while(true)
    {
     try
     {
    	 /////////////////////////////////////////////////// 
         //ENVIAR PING MULTICAST
    	 ///////////////////////////////////////////////////
    	 
    	 // FORMATO DEL MENSAJE
    	 /*
    	  * nº secuencia (int)
    	  * ttl envio: (int)
    	  * tiempo local de envío: (long)
    	  * payload number: (int)
    	  * payload: (n bytes)
    	  * 
    	  */
    	 bufSend.reset();
    	 
    	 long newCurrentTime = System.currentTimeMillis();
    	 if (newCurrentTime-sendTime>1000L)
    	 {
    	 
    	 //enviar datos 
    	 secuencia=secuencia+1;
    	 bufSend.addInt(secuencia);
    	 bufSend.addInt(this.TTLSesion);
    	 sendTime = System.currentTimeMillis();
    	 bufSend.addLong(sendTime);
    	 bufSend.addInt(this.payload_size);
    	 bufSend.addBytes(payloadSend, 0, payloadSend.getLength());
    	
    	    	 
         DatagramPacket hi = new DatagramPacket(bufSend.getBuffer(),bufSend.getLength(),dirIPMcast.getInetAddress(),dirIPMcast.getPort());
         socket.send(hi);
         Log.log("SEND",">> packet nº "+secuencia+ " with TTL="+this.TTLSesion+" localtime="+sendTime+"ms");
    	 }
         
        /////////////////////////////////////////////////// 
        //RECIBIR PING MULTICAST
        ///////////////////////////////////////////////////
         
         
    	//Leer datos... espera máxima de 1seg
        socket.receive(recv);
        long RecvCurrentTime = System.currentTimeMillis();
        recvPuerto = recv.getPort();
        sizeRcv = recv.getLength();
        if(sizeRcv > 0)
        {
        	        	 
        	bufRecv.setLength(sizeRcv);
        	bufRecv.setOffset(0);
        	
        	int offset = 0;
        	// nº secuencia (int)
        	int recvSecuencia = (int) bufRecv.getInt(offset);
        	offset=offset+4;
        	
        	
        	// ttl envio: (int)
        	int recvTTLEnvio = (int) bufRecv.getInt(offset);
        	offset=offset+4;
        	
        	
        	// tiempo local de envío: (long)
        	long recvSendTime = bufRecv.getLong(offset);
        	offset=offset+8;
        	        	
        	// payload number: (int)
        	int recvPayloadSize = (int) bufRecv.getInt(offset);
        	offset=offset+4;
        	
        	
        	// payload: (n bytes)
        	Buffer RecvPayloadBuffer = new Buffer(recvPayloadSize);
        	RecvPayloadBuffer.addBytes(bufRecv, offset,recvPayloadSize);
        	
        	//We assume clock's hosts are synchronized by NTP
        	long DiffTime = RecvCurrentTime - recvSendTime; 
        	
        	
        	        	
        	Log.log("RECV","<< packet nº "+recvSecuencia+" from "+recv.getSocketAddress()+" payload="+recvPayloadSize+" bytes time:"+DiffTime+"ms");
        	
        	
        }

        //Address add = new Address(recv.getAddress(),puerto);
        //contador =contador+1;
        
     }
     catch(SocketTimeoutException z)
     {
        // today = new Date();
        // output = formatter.format(today);
        //Log.log("*PERDIDA*",output+" No se han recibido paquetes en 5 seg!");
        // logger("[mPingReceiver2]: *PERDIDA MULTICAST*  No se han recibido paquetes en 5 seg! desde surapb");
     }

    }


}
catch(IOException e)
{

 error(e.getMessage());
}

//Limpiar....
finally
{
  //Cerrar el Socket
  close();

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
  socket.leaveGroup(dirIPMcast.getInetAddress());
  socket.close();
}
catch(IOException e)
{
   error(e.getMessage());
}
}


/**
* Método main.
* @param args
*/
public static void main(String[] args)
{
//Comprobar parámetros
if(args.length != 4)
{
  uso();
  return;
}

try
{
    mping mping = new mping(args[0],args[1],args[2],args[3]);
    mping.run();
}
catch(IOException io)
{
  System.out.print(io.getMessage());
}
}




//==========================================================================
/**
* Resumen
*/
private void resumenTransferencia(long lTiempoInicio, long lBytesTransmitidos)
{
  long lHoras = 0;
  long lMinutos = 0;
  long lSegundos = 0;
  long lMSegundos = 0;
  long lTiempo = System.currentTimeMillis() - lTiempoInicio;
  double dKB_seg =0;

  String mensaje = "Transmitido "+lBytesTransmitidos+" bytes en ";

  dKB_seg = ((double)(lBytesTransmitidos)/(double)(lTiempo)) *1000;
  dKB_seg = (dKB_seg / 1024);

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


  System.out.println("");
  System.out.println("");

  if (dKB_seg > 1)
  {
    int iParteEntera = (int)(dKB_seg );
    int iParteDecimal = (int)(dKB_seg *100)%100;
   // Log.log(mn,mensaje);
   // Log.log(mn,"Ratio Transferencia: "+iParteEntera+"."+iParteDecimal+" KB/Seg");
  }
  else
  {
    int i = (int)(dKB_seg * 100);
    //Log.log(mn,mensaje);
    //Log.log(mn,"Ratio Transferencia: 0."+i+" KB/Seg");
  }

}






/**
* Imprime el mensaje de uso de la aplicación
*/
private static void uso()
{
banner();
System.out.println("Use: java mutil.mPing <ip multicast address> <port> <ttl> <datagram size>");
System.out.println("Example: java mutil.mPing 224.1.1.1 2020 32 32");
System.out.println("");
}


/**
* Imprime el mensaje de error
* @param sError
*/
private void error(String sError)
{
System.out.println("=========================================");
System.out.print("Error: ");
System.out.println(sError);

}



    /**
     * Manda el mensaje al logger de consola!!!
     * @param <any> String
     */
    private void logger(String log)
    {
               //Llamar a logger
               try
               {
                   Runtime rt = Runtime.getRuntime() ;
                   String mensaje = "logger "+log;
                   //Log.log("logger",mensaje);
                   String comando[] = { "logger", log };
                   Process p = rt.exec(comando) ;
                   InputStream in = p.getInputStream() ;
                   OutputStream out = p.getOutputStream ();
                   InputStream err = p.getErrorStream() ;


                   //do whatever you want
                   //some more code
                   //p.wait(2000);
                   p.waitFor();
                   //log("----------------------------------------------------------------------");


               }catch(Exception exc )
               {
                   /*handle exception*/
                   System.err.println(exc.getMessage());
               }

    }

}

