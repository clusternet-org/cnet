//============================================================================
//
//	Copyright (c) 1999,2014 . All Rights Reserved.
//
//----------------------------------------------------------------------------
//
//	Fichero: SocketClusterNetImp.java  1.0 25/09/99
//
//	Descripción: Clase SocketClusterNetImp.
//
//
// 	Authors: 
//		 Alejandro García-Domínguez (alejandro.garcia.dominguez@gmail.com)
//		 Antonio Berrocal Piris (antonioberrocalpiris@gmail.com)
//
//  Historial: 
//  07.04.2015 Changed licence to Apache 2.0     
//
//  This file is part of ClusterNet 
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
//----------------------------------------------------------------------------



package org.clusternet;

import java.util.Vector;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.TreeMap;
import java.io.*;
import java.net.UnknownHostException;
import java.net.InetAddress;

/**
 * <STRONG><b>Clase SocketClusterNetImp.</STRONG><br>
 * Implementación del  API común presentada por la clase ClusterNet.</b><br>
 * <br>
 * SocketClusterNetImp crea dos canales:<br>
 *  - Un canal Multicast (IPMulticast + Puerto)<br>
 *  - Un canal Unicast   (IPUnicast Ipv4 + Puerto)<br>
 *
 * <br>Esta clase no es thread-safe.
 * @version  1.0
 * @author M. Alejandro García Domínguez
 * <A HREF="mailto:alejandro.garcia.dominguez@gmail.com">(alejandro.garcia.dominguez@gmail.com)</A><p>
 *			   Antonio Berrocal Piris
 */
class SocketClusterNetImp
{

  /** ClusterTimer */
  private ClusterTimer clusterTimer = null;


  /**
   * Un flag para indicar si ha ocurrido un error irrecuperable.
   */
  private boolean  error = true;

  /**
   *  Modo del SocketClusterNetImp: MODE_RELIABLE o MODE_NO_RELIABLE.
   */
  private int modo = 0;

  /**
   * Un flag para indicar si el grupo multicast está activo o no.
   * Este flag se activará cuando el host pertenezca a un Grupo Local y esté
   * en plena disposición de enviar y recibir datos Multicast acorde al
   * protocolo ClusterNet.
   */
  private boolean grupoMcastActivo = false;

  /**
   * La dirección de esta máquina
   */
  private Address  addressLocal = null;

  /**
   * La interfaz de salida utilizada
   */
  private Address  addressInterfaz = null;


  /**
   * El Channel Multicast para recibir/enviar los datos multicast.
   */
  private MulticastChannel  canalMcast = null;

  /**
   * El Channel Unicast para recibir/enviar los datos unicast.
   */
  private UnicastChannel  unicastChannel = null;

  /**
   * ClusterNetOutputStream
   */
  private ClusterNetOutputStream  mCastOutputStream = null;

  /**
   * ClusterNetInputStream
   */
  private ClusterNetInputStream  mCastInputStream = null;


  /**
   * TTL: El TTL utilizado en la sesión
   */
  private short TTLSesion = 0;

  /**
   * El objeto ReceiveHandler de la aplicación.
   */
  private ReceiveHandler  receiveHandler = null;

  /**
   * El argumento del objeto ReceiveHandler de la aplicación
   */
  private int  receiveHandlerArg = 0;

  /** Lista de objetos PTMFIDGLListeners  */
  private LinkedList  listaPTMFIDGLListeners = null;

  /**  Lista de objetos PTMFID_SOCKETListeners  */
  private LinkedList  listaPTMFID_SocketListeners = null;

  /**  Lista de objetos PTMFConexionListeners  */
  private LinkedList  listaPTMFConexionListeners = null;

  /**  Lista de objetos PTMFDatosRecibidosListeners  */
  private LinkedList  listaPTMFDatosRecibidosListeners = null;

  /**  Lista de objetos PTMFErrorListeners  */
  private LinkedList  listaPTMFErrorListeners = null;

  /**
   * Cola de Recepcion (contenido: Objetos RegistroEmisor_Buffer)
   */
  private RxQueue rxQueue = null;

  /**
   * Cola de Emision (contenido: Objetos Buffer)
   */
  private TXQueue tXQueue = null;

  /**
   * Vector de Registros CGL. Encapsula TPDUs CGL recibidos y la dirección
   * del emisor.
   */
  private Vector vectorRegistroCGL = null;

  /**
   * Vector de Registros Datos. Encapsula TPDUs de Datos recibidos y la dirección
   * del emisor.
   */
  Vector vectorRegistroDatos = null;


  /** Último número de secuencia recibido del TPDU CGL */
  private int N_SECUENCIA_ULTIMO = 0;

  /** Dirección del emisor del Último TPDU CGL recibido*/
  private Address addressUltimoEmisor = null;

  /**
   * Thread de procesamiento de TPDUs CGL
   */
  private CGLThread cglThread = null;

  /**
   * Thread de procesamiento de TPDUs de Datos
   */
  private DataThread dataThread = null;

  /**
   * Semáforo para la aplicación
   */
  private Semaforo semaforoAplicacion = null;


  /**

   * Dirección del emisor del Último TPDU CGL recibido,
   *  utilizada junto a N_SECUENCIA_ULTIMO_TPDU_CGL para evitar procesar múltiples
   * TPDUCGL del mismo emisor debido a la redundancia en los mensajes CGL.
   */
  private Address addressUltimoEmisorTPDUCGL = null;


  private String newline = "/n";


  /**
   * ChannelCallback para la recepción de los datos enviados por multicast.
   */

  private ChannelCallback canalCallbackMulticast = new ChannelCallback ()

        {

          //==========================================================================

          /**
           * El método callback del canal. Implementación de la interfaz ChannelCallback.
           * Este método será llamado cuando se reciban datos en el Channel Multicast.
           * @param arg Un argumento para el método.
           * @param buf un buffer que contiene los datos recibidos.  El handler debe de
           * copiar los datos del buffer fuera de este objeto <b>sin modificar el
           * buffer. </b>
           * @param src Un objeto Address conteniendo la dirección fuente de los datos.
           * El handler debe de copiar la dirección fuera de este objeto <b>sin modificar
           * los datos.</b>
           */// Es usada por unicast y multicast
         public void canalCallback(int arg, Buffer buf, Address src)

           {
            final String  mn = "SocketClusterNetImp.canalCallback(int,Buffer,Address)";
            byte  tipo;

            try
            {

            // ALEX: 27/04/2003  --> Creo que NO, es necesario el descarte del loop
            // Tengo que reaactivar la entrada del loop para que en el caso de que halla un único socket
            // por grupo (clusterGroupID) el socket envíe los asentimientos correspondiente, es necesario por tanto
            // escuchar los TPDUS que se envían y procesarlos

            // DESCARTAR LOS PAQUETES ENVIADOS POR "NOZZOTROS", POR ESTE SOCKET, EN JAVA NO HAY
            // AÚN UN MECANISMO PARA DESHABILITAR EL LOOP DE LOS DATAGRAMAS MULTICAST
            // HACERLO EN JNI ES UN POCO "GUARRO".
            if( /*(this.cglThread.getEstadoCGL()!= ClusterNet.ESTADO_CGL_MONITOR)
              && */
              (getAddressLocal().equals(src.getInetAddress()))
              && (TPDU.getPuertoUnicast(buf)==getAddressLocal().getPort()))
            {
              Log.debug(Log.SOCKET,"SocketClusterNetImp.canalCallback","Datagrama DESCARTADO Emisor: "+src/*+"Buffer: "+buf*/);
              //Log.log("SocketClusterNetImp.canalCallback","Datagrama DESCARTADO Emisor: "+src/*+"Buffer: "+buf*/);

              ////Log.debug(Log.SOCKET,"SocketClusterNetImp.canalCallback","ID_SOCKET recibido:"+(int)TPDU.getIdSocket(buf)+" ID_SOCKET:"+(int)this.ID_SOCKET);
              return;
            }
            else
            {
              
               Log.debug(Log.SOCKET,"SocketClusterNetImp.canalCallback","Datagrama ACEPTADO Emisor: "+src/*+"Buffer: "+buf*/);
               Log.debug(Log.SOCKET,"SocketClusterNetImp.canalCallback","Dirección Local: "+getAddressLocal());  
               Log.debug(Log.SOCKET,"SocketClusterNetImp.canalCallback","Dirección Emisor: "+src.getInetAddress());  
               Log.debug(Log.SOCKET,"SocketClusterNetImp.canalCallback","Puerto Local: "+getAddressLocal().getPort());  
               Log.debug(Log.SOCKET,"SocketClusterNetImp.canalCallback","Puerto Emisor: "+TPDU.getPuertoUnicast(buf));  
            }

            //Log.debug(Log.SOCKET,"SocketClusterNetImp.canalCallback","Arg: "+arg+" Emisor: "+src/*+"Buffer: "+buf*/);


              //
              // Determinar el tipo del TPDU y actuar conforme a ello.
              //
              switch (tipo = TPDU.getTipo(buf))
              {
                case ClusterNet.TPDU_CGL:
                  //
                  // Verificar el modo del socket
                  //
                  if(modo==ClusterNet.MODE_RELIABLE || modo==ClusterNet.MODE_MONITOR || modo == ClusterNet.MODE_DELAYED_RELIABLE)
                    procesarTPDUCGL(buf,src);
                  break;

                case ClusterNet.TPDU_DATOS:
                 //
                 // Verificar que el grupo está activo.
                 //
                 // indico si ha sido recibido por unicast
                 procesarTPDUDatos(buf, src, false);
                 break;

                default:
                  Log.log(mn, "Tipo de TPDU recibido ("+tipo+") no válido.");
              }
            }
            catch(ClusterNetExcepcion e) {return;}
          }
         };



  /**

   * ChannelCallback para la recepción de los datos enviados por unicast.

   */

  private ChannelCallback canalCallbackUnicast = new ChannelCallback ()

        {

          //==========================================================================

          /**
           * El método callback del canal. Implementación de la interfaz ChannelCallback.
           * Este método será llamado cuando se reciban datos en el Channel Multicast.
           * @param arg Un argumento para el método.
           * @param buf un buffer que contiene los datos recibidos.  El handler debe de
           * copiar los datos del buffer fuera de este objeto <b>sin modificar el
           * buffer. </b>
           * @param src Un objeto Address conteniendo la dirección fuente de los datos.
           * El handler debe de copiar la dirección fuera de este objeto <b>sin modificar
           * los datos.</b>
           */// Es usada por unicast y multicast
         public void canalCallback(int arg, Buffer buf, Address src)

           {
            final String  mn = "SocketClusterNetImp.canalCallback(int,Buffer,Address)";
            byte  tipo;

            try
            {

            // DESCARTAR LOS PAQUETES ENVIADOS POR "NOZZOTROS", POR ESTE SOCKET, EN JAVA NO HAY
            // AÚN UN MECANISMO PARA DESHABILITAR EL LOOP DE LOS DATAGRAMAS MULTICAST
            // HACERLO EN JNI ES UN POCO "GUARRO".
            if( /*(this.cglThread.getEstadoCGL()!= ClusterNet.ESTADO_CGL_MONITOR)
              && */(getAddressLocal().getInetAddress().equals(src.getInetAddress()))
              && (TPDU.getPuertoUnicast(buf)==getAddressLocal().getPort()))
            {
              //Log.debug(Log.SOCKET,"SocketClusterNetImp.canalCallback","Datagrama DESCARTADO Emisor: "+src/*+"Buffer: "+buf*/);
              ////Log.debug(Log.SOCKET,"SocketClusterNetImp.canalCallback","ID_SOCKET recibido:"+(int)TPDU.getIdSocket(buf)+" ID_SOCKET:"+(int)this.ID_SOCKET);
              return;
            }

            //Log.debug(Log.SOCKET,"SocketClusterNetImp.canalCallback","Arg: "+arg+" Emisor: "+src/*+"Buffer: "+buf*/);


              //
              // Determinar el tipo del TPDU y actuar conforme a ello.
              //
              switch (tipo = TPDU.getTipo(buf))
              {
                case ClusterNet.TPDU_CGL:
                  //
                  // Verificar el modo del socket
                  //
                  if(modo==ClusterNet.MODE_RELIABLE || modo==ClusterNet.MODE_MONITOR || modo == ClusterNet.MODE_DELAYED_RELIABLE)
                    procesarTPDUCGL(buf,src);
                  break;

                case ClusterNet.TPDU_DATOS:
                 //
                 // Verificar que el grupo está activo.
                 //
                 // indico si ha sido recibido por unicast
                 procesarTPDUDatos(buf, src, true);
                 break;

                default:
                  Log.log(mn, "Tipo de TPDU recibido ("+tipo+") no válido.");
              }
            }
            catch(ClusterNetExcepcion e) {return;}
          }
         };



  //==========================================================================
  /**
   * Constructor genérico.
   * Abre el socket y añade el emisor a un grupo Multicast, cerrando primero
   * cualquier grupo activo. Usa la interfaz que se especifica para enviar
   * y recibir los datos.<br>
   * Solo un grupo multicast puede estar activo en un determinado momento.
   * @param grupoMulticast Un objeto Address con la dirección del grupo
   *  multicast al que se unirá (join) este emisor y con el puerto ClusterNet al que
   *  se enviarán/recibirán los datos multicast.
   * @param interfaz Interfaz por la que se envian/reciben los datos.
   * @param ttl El valor del TTL usado para todos los paquetes multicast.
   * <ul>
   * <il>ttl 1   = Subred
   * <il>ttl 8   = Local
   * <il>ttl 32  = Regional
   * <il>ttl 48  = Nacional
   * <il>ttl 64  = Europea
   * <il>ttl 128 = Internacional
   * </ul>
   * @param modo Establece el modo de fiailidad del protocolo:
   * <UL>
   * <IL><b>FIABLE: MODE_RELIABLE</B>
   * <IL><b>NO FIABLE: MODE_NO_RELIABLE</B>
   * </UL>
   * @param cipher Objeto javax.crypto.Cipher utilizado para codificar
   * @param unCipher Objeto javax.crypto.Cipher utilizado para descodificar.
     * @exception ClusterNetInvalidParameterException Se lanza si el parámetro modo
   *  no es correcto
   * @exception ClusterNetExcepcion Se lanza si ocurre un error.
   * @see ClusterNet
   */
  SocketClusterNetImp(Address grupoMulticast, Address interfaz, byte ttl,
    int modo_fiabilidad,ClusterNetConnectionListener listener,
     javax.crypto.Cipher cipher, javax.crypto.Cipher unCipher) throws ClusterNetInvalidParameterException, ClusterNetExcepcion
  {
   super();

   final String mn = "SocketClusterNetImp.SocketPTMFImp (grupoMcst,interfaz,ttl,modo_Fiabilidad)";

   try
    {

     switch(modo_fiabilidad)
     {
      case ClusterNet.MODE_RELIABLE:
        this.modo = modo_fiabilidad;
        //Log.debug(Log.SOCKET,"SocketClusterNetImp.SocketPTMFImp"," --> Modo Fiable");
        break;
      case ClusterNet.MODE_DELAYED_RELIABLE:
        this.modo = modo_fiabilidad;
        //Log.debug(Log.SOCKET,"SocketClusterNetImp.SocketPTMFImp"," --> Modo Fiable Retrasado");
        break;
      case ClusterNet.MODE_NO_RELIABLE:
        //Log.debug(Log.SOCKET,"SocketClusterNetImp.SocketPTMFImp"," --> Modo NO Fiable");
        this.modo = modo_fiabilidad;
        break;
      case ClusterNet.MODE_NO_RELIABLE_ORDERED:
        this.modo = modo_fiabilidad;
        //Log.debug(Log.SOCKET,"SocketClusterNetImp.SocketPTMFImp"," --> Modo NO Fiable Ordenado");
        break;

      case ClusterNet.MODE_MONITOR:
        //Log.debug(Log.SOCKET,"SocketClusterNetImp.SocketPTMFImp"," --> Modo MONITOR");
        this.modo = modo_fiabilidad;
        break;
      default:
        throw new ClusterNetInvalidParameterException("SocketClusterNetImp(int)","El modo del Socket es incorrecto.");
     }


     //--------------------------------------------------------------
     // Crear clusterTimer
     //
     clusterTimer = new ClusterTimer();

     //--------------------------------------------------------------
     // Crear Canales Multicast, Unicast  y  establecer ttl y callback.
     //

      if(interfaz != null)
      {
        //-----------------------------------------------------------------
        //Nuevo Channel Multicast, enlazar con la dirección especificada.
        canalMcast = MulticastChannel.getInstance(grupoMulticast,interfaz,cipher,unCipher);

        //-----------------------------------------------------------------
        //Nuevo Channel Unicast
        unicastChannel = UnicastChannel.getInstance(new Address(InetAddress.getLocalHost(),0) ,interfaz,cipher,unCipher);
      }
      else
      {
        //-----------------------------------------------------------------
        //Nuevo Channel Multicast, enlazar con la dirección especificada.
        canalMcast = MulticastChannel.getInstance(grupoMulticast,null,cipher,unCipher);

        //-----------------------------------------------------------------
        //Nuevo Channel Unicast, enlazar automáticamente, no se especifica dirección.
        unicastChannel = UnicastChannel.getInstance(new Address(InetAddress.getLocalHost(),0),null,cipher,unCipher);
      }

      Log.debug(Log.SOCKET,"SocketClusterNetImp","Channel Multicast: "+this.canalMcast);
      Log.debug(Log.SOCKET,"SocketClusterNetImp","Channel Unicast: "+this.unicastChannel);

      //-----------------------------------------------------------------
      // Establecer TTL.....
      canalMcast.setTTL(ttl);
      this.TTLSesion = ttl;

      //-----------------------------------------------------------------
      // Establecer Address.....
      this.addressInterfaz = interfaz;
      if (this.addressInterfaz != null)
              Log.debug(Log.SOCKET,"SocketClusterNetImp","Dirección Interfaz: "+this.addressInterfaz);
      this.addressLocal = new Address(InetAddress.getLocalHost(),this.unicastChannel.getAddressUnicast().getPort());
      this.addressUltimoEmisor = new Address();

      //-----------------------------------------------------------------
      // Establecer callback.....
      canalMcast.setCallback(this.canalCallbackMulticast,0);
      unicastChannel.setCallback(this.canalCallbackUnicast,1); // IMPORTANTE: POR EL ARGUMENTO SÉ CUAL ES EL UNICAST

      //-----------------------------------------------------------------
      // Crear colas de emisión y de recepción....
      this.tXQueue = new TXQueue(ClusterNet.TAMAÑO_DEFECTO_COLA_EMISION,this);
      this.rxQueue = new RxQueue(ClusterNet.TAMAÑO_DEFECTO_COLA_RECEPCION,this);

      //-----------------------------------------------------------------
      //Lista de Listeners...
      this.listaPTMFIDGLListeners = new LinkedList();
      this.listaPTMFID_SocketListeners = new LinkedList();
      this.listaPTMFConexionListeners = new LinkedList();
      this.listaPTMFDatosRecibidosListeners = new LinkedList();
      this.listaPTMFErrorListeners = new LinkedList();


      //Añadir listener inicial...
      if (listener != null)
        this.listaPTMFConexionListeners.add(listener);

      // EN EL MODO FIABLE CREAR....
      if (this.modo == ClusterNet.MODE_RELIABLE || this.modo == ClusterNet.MODE_DELAYED_RELIABLE)
      {
        // Flujos de Entrada y de Salida
        this.mCastOutputStream = new ClusterNetOutputStream(this);
        this.mCastInputStream = new ClusterNetInputStream(this);

        //-----------------------------------------------------------------
        // Crear los mutex/semáforos necesarios
        //
        // Semáforo binario para para la aplicación....
        this.semaforoAplicacion = new Semaforo(true,0);

        //-----------------------------------------------------------------
        // Crear el vector de registro CGL.
        //
        this.vectorRegistroCGL = new Vector ();
      }
      //-----------------------------------------------------------------
      // Crear el vector de registro Datos.
      //
      this.vectorRegistroDatos = new Vector ();

      //Notificar ...
      this.sendPTMFEventConexion("Channel Multicast: "+this.canalMcast);
      this.sendPTMFEventConexion("Channel Unicast: "+this.unicastChannel);
      this.sendPTMFEventConexion("ClusterNet: "+this.getID_Socket());

      // EN EL MODO FIABLE CREAR....
      if (this.modo == ClusterNet.MODE_RELIABLE || this.modo == ClusterNet.MODE_DELAYED_RELIABLE)
      {
        //-----------------------------------------------------------------
        // Crear el Thread de procesamiento CGL (CGLThread).
        //
        this.cglThread = new CGLThread(this);

        //----------------------------------------------------------------
        // Iniciar el Thread de procesamiento CGL (CGLThread).
        //
        this.cglThread.start ();
      }


      //-----------------------------------------------------------------
      // Unirse a un grupo-local si el modo es MODE_RELIABLE.
      //
      if (this.modo == ClusterNet.MODE_RELIABLE || this.modo == ClusterNet.MODE_DELAYED_RELIABLE)
        joinGrupoLocal();
      else
      {
         //Grupo McastActivo activo
         this.grupoMcastActivo = true;

         //if (this.modo == ClusterNet.MODE_MONITOR)
         // this.cglThread.setEstadoCGL(ClusterNet.ESTADO_CGL_MONITOR);
      }

      //Log.debug(Log.SOCKET,"SocketClusterNetImp.SocketPTMFImp"," --> join "+grupoMulticast);

      //-----------------------------------------------------------------
      // Crear e iniciar el Thread de procesamiento de Datos (DataThread).
      //
      this.dataThread = new DataThread (this,modo_fiabilidad);
      this.dataThread.start();
      ////Log.debug (Log.SOCKET,mn,"Hilo datos --> OK");

    }
    catch(UnknownHostException e)
    {
       throw new ClusterNetExcepcion(e.getMessage());
    }
    catch(ClusterNetInvalidParameterException e)
    {
       throw new ClusterNetExcepcion(e.getMessage());
    }
    catch(IOException e)
    {
       throw new ClusterNetExcepcion(e.getMessage());
    }

  };


  //==========================================================================
  /**
   * Una vez llamada esta función no se podrá enviar ningún byte más.
   */
  void closePTMFEmision ()
  {
    this.tXQueue.setEncolarDatos (false);
    // Alex: Puedes enterar desde la propia cola.

    this.dataThread.finEmision ();
  }


  //==========================================================================
  /**
   * Cierra el socket. Quita al emisor (leave) del grupo multicast activo,
   * cualquier dato recibido del grupo multiast se perderá.<br>
   * Sólo un grupo multicast puede estar activo en cualquier momento.
   * @exception ClusterNetExcepcion Se lanza si ocurre algún error cerrando el socket.
   */
  void closePTMF(boolean estable) throws ClusterNetExcepcion
  {
     //Log.debug(Log.SOCKET,"SocketClusterNetImp.closePTMF"," --> close");

   // No encolar más datos en la cola de emisión.
   if (this.tXQueue!=null)
        this.tXQueue.setEncolarDatos (false);

   this.desactivarRecepcion();

   try
   {
     //
     // Cerrar thread Datos
     //
     if (dataThread != null)
      {
       dataThread.stopThread(estable);

       //
       // Esperar hasta que finalice el thread de datos
       //
       dataThread.join ();
      }

      // EN EL MODO FIABLE CERRAR....
      if (this.modo == ClusterNet.MODE_RELIABLE || this.modo == ClusterNet.MODE_DELAYED_RELIABLE)
      {
           //
           // Cerrar thread CGL
           //
           if (cglThread!=null)
            {
              cglThread.stopThread();
              //
              // Esperar hasta que finalice el thread
              cglThread.join();
            }

           if (this.vectorRegistroCGL!=null)
            {
             this.vectorRegistroCGL.removeAllElements();
             this.vectorRegistroCGL = null;
            }

           this.semaforoAplicacion = null;

           //Cerrar Flujos de Entrada/Salida
           if (this.mCastInputStream!=null)
            {
              this.mCastInputStream.close();
              this.mCastOutputStream.close();
            }
           if (this.mCastOutputStream!=null)
            {
             this.mCastOutputStream = null;
             this.mCastInputStream = null;

            }


     }

     //Grupo McastActivo inactivo
    this.grupoMcastActivo = false;

    //
    // Cerrar canal
    //
    if (canalMcast != null)
     {
      this.canalMcast.close();
      this.canalMcast = null;
     }
    if (unicastChannel != null)
     {
      this.unicastChannel.close();
      this.unicastChannel = null;
     }
    this.modo = 0;


    this.TTLSesion = 0;
    this.addressLocal = null;
    this.addressUltimoEmisorTPDUCGL = null;



    clusterTimer.reiniciar();
    this.cglThread = null;
   }
   catch(IOException e){return;}
   catch(InterruptedException ie){;}
  };


  //==========================================================================
  /**
   * Cierra el socket. Quita al emisor (leave) del grupo multicast activo,
   * cualquier dato recibido del grupo multiast se perderá.<br>
   * Sólo un grupo multicast puede estar activo en cualquier momento.
   * @exception ClusterNetExcepcion Se lanza si ocurre algún error cerrando el socket.
   */
  void closePTMF() throws ClusterNetExcepcion
  {
    closePTMF(false); //NO estable.
  }


  //==========================================================================
  /**
   * Devuelve un objeto ClusterNetInputStream, un flujo de entrada para este socket.
   * @return Un flujo de entrada para leer bytes desde este socket.
   * @exception IOException Se lanza si no se puede crear el flujo de entrada.
   */
  ClusterNetInputStream getMulticastInputStream() throws IOException
  {
    return this.mCastInputStream;
  }

  //==========================================================================
  /**
   * Devuelve un objeto ClusterNetOutputStream, un flujo de salida para este socket.
   * @return Un flujo de salida para escribir bytes en este socket.
   * @exception IOException Se lanza si no se puede crear el flujo de salida.
   */
  ClusterNetOutputStream getMulticastOutputStream() throws IOException
  {
    if(!this.tXQueue.esActiva())
      throw new IOException("No se pudo crear el Flujo ClusterNetOutputStream."+newline+" La emisión  de datos sobre el ClusterNet ha sido desactivada.");

    if(this.mCastOutputStream.isClose())
      this.mCastOutputStream = new ClusterNetOutputStream(this);

    return this.mCastOutputStream;
  }



 //==========================================================================
  /**
   * Devuelve un boolean que indica si el grupo Multicast está activo.
   * @return True si el grupo multicast está activo, false en caso contrario.
   */
   boolean isGrupoMcastActivo()  {  return this.grupoMcastActivo; }

  //==========================================================================
  /**
   * Establece el valor del booleano grupoMcastActivo.
   * @param Un valor booleano
   */
  void setGrupoMcastActivo(boolean set)  {  this.grupoMcastActivo=set; }

  //==========================================================================
  /**
   * Devuelve el objeto semaforoAplicacion
   * @return Un objeto Semaforo
   */
   Semaforo getSemaforoAplicacion()  {  return this.semaforoAplicacion; }

  //==========================================================================
  /**
   *  Devuelve el thread CGLThread
   * @return CGLThread
   */
   CGLThread getCGLThread() { return this.cglThread;}

  //==========================================================================
  /**
   * Devuelve el identificador (ID_SOCKET) de este socket
   * @return el ID_SOCKET de este socket ClusterNet.
   */
   ClusterMemberID getID_Socket() {return this.getAddressLocal().toID_Socket(); }


  //==========================================================================
  /**
   * Este método devuelve el ClusterGroupID de este socket.
   * @return Objeto ClusterGroupID
   */
  ClusterGroupID getIDGL ()
  {
    if(this.modo == ClusterNet.MODE_RELIABLE || this.modo == ClusterNet.MODE_DELAYED_RELIABLE)
      return this.cglThread.getIDGL();
    else
      return this.addressLocal.toIDGL();
  }

  //==========================================================================
  /**
   *  Devuelve el objeto AddressLocal.
   * @return Objeto Address
   */
   Address getAddressLocal() {  return this.addressLocal;  }


  //==========================================================================
  /**
   * Devuelve el objeto canalMcast.
   * @return Objeto MulticastChannel
   */
   MulticastChannel getCanalMcast()  {   return this.canalMcast;  }

  //==========================================================================
  /**
   * Devuelve el objeto unicastChannel
   * @return Objeto UnicastChannel
   */
   UnicastChannel getCanalUnicast() {  return this.unicastChannel; }

 //==========================================================================
  /**
   * Devuelve el objeto ClusterTimer
   * @return Objeto ClusterTimer
   */
   ClusterTimer getTemporizador() {  return this.clusterTimer; }

  //==========================================================================
  /**
   * Devuelve el TTL de la sesión
   * @return Un short indicando el TTL de la sesión.
   */
   short getTTLSesion()  {   return this.TTLSesion;  }

  //==========================================================================
  /**
   * Devuelve la dirección de la interfaz de salida: addressInterfaz
   * @return Objeto Address con la dirección de la Interfaz.
   */
   Address getAddressInterfaz()  {   return this.addressInterfaz;  }

  //==========================================================================
  /**
   * Devuelve el vectorRegistroCGL
   * @return Un Vector
   */
   Vector getVectorRegistroCGL() { return this.vectorRegistroCGL; }

  //==========================================================================
  /**
   * Devuelve la lista de Emision de Objetos RegistroEmisor_Buffer.
   * @return El objeto Vector
   */
   TXQueue getColaEmision()  {return this.tXQueue;  }

  //==========================================================================
  /**
   * Devuelve la lista de Recepcion de Objetos RegistroEmisor_Buffer.
   * @return El objeto Vector
   */
   RxQueue getColaRecepcion()  {return this.rxQueue;  }


  //==========================================================================
  /**
   * Devuelve el tamaño del buffer de emisión.
   * @return un int con el tamaño del buffer de emisión
   */
  int getCapacidadColaEmision()
  {
    if (this.tXQueue == null)
      return -1;

    return this.tXQueue.getCapacidad();
  }

  //==========================================================================
  /**
   * Devuelve los id_socket de todos los emisores actuales.
   * @return un int con el tamaño del buffer de emisión
   */
  TreeMap getID_SocketEmisores()
  {
    if (this.modo == ClusterNet.MODE_RELIABLE || this.modo == ClusterNet.MODE_DELAYED_RELIABLE)
      return null;

    return this.dataThread.getID_SocketEmisores();
  }




  //==========================================================================
  /**
   * Establece el tamaño del buffer de emisión
   * @param tamaño del buffer de emisión
   */
  void setCapacidadColaEmision(int size)
  {
    if(this.tXQueue!= null)
      this.tXQueue.setCapacidad(size);
  }

  //==========================================================================
  /**
   * Devuelve el tamaño del buffer de recepción
   * @return un int con el tamaño del buffer de recepción
   */
  int getCapacidadColaRecepcion()
  {
    if (this.rxQueue == null)
      return -1;

    return this.rxQueue.getCapacidad();
  }

  //==========================================================================
  /**
   * Envía un evento ClusterNetEventConecction con una cadena informativa.
   * @param mensaje Mensaje Informativo
   */
   void sendPTMFEventConexion(String mensaje)
   {
    //Log.log("Enviar Evento Conexión...."," nº listener: "+this.listaPTMFConexionListeners.size());

    if (this.listaPTMFConexionListeners.size() > 0)
    {
     ClusterNetEventConecction evento = new ClusterNetEventConecction(this,mensaje);
     Iterator iterator = this.listaPTMFConexionListeners.listIterator();
     while(iterator.hasNext())
     {
        ClusterNetConnectionListener ptmfListener = (ClusterNetConnectionListener)iterator.next();
        ptmfListener.actionNewConnection(evento);
     }
    }
   }

  //==========================================================================
  /**
   * Envía un evento ClusterNetEventError  con una cadena informativa.
   * @param mensaje Mensaje Informativo
   */
   void sendPTMFEventError(String mensaje)
   {
    if (this.listaPTMFErrorListeners.size() != 0)
    {
     ClusterNetEventError evento = new ClusterNetEventError(this,mensaje);

     Iterator iterator = this.listaPTMFErrorListeners.listIterator();
     while(iterator.hasNext())
     {
        ClusterNetErrorListener ptmfListener = (ClusterNetErrorListener)iterator.next();
        ptmfListener.actionError(evento);
     }
    }
   }


  //==========================================================================
  /**
   * Envía un evento ClusterNetEvent del tipo EVENTO_IDGL
   * con una cadena informativa.
   * @param mensaje Mensaje Informativo
   * @param clusterGroupID Objeto ClusterGroupID que ha sido añadido al grupo
   */
   void sendPTMFEventAddIDGL(String mensaje,ClusterGroupID clusterGroupID)
   {
    if (this.listaPTMFIDGLListeners.size() != 0)
    {
     ClusterNetEventGroup evento = new ClusterNetEventGroup(this,mensaje,clusterGroupID,true);

     Iterator iterator = this.listaPTMFIDGLListeners.listIterator();
     while(iterator.hasNext())
     {
        ClusterNetGroupListener ptmfListener = (ClusterNetGroupListener)iterator.next();
        ptmfListener.actionPTMFIDGL(evento);
     }
    }
   }

  //==========================================================================
  /**
   * Envía un evento ClusterNetEvent del tipo EVENTO_IDGL
   * con una cadena informativa.
   * @param mensaje Mensaje Informativo
   * @param clusterGroupID Objeto ClusterGroupID que ha sido eliminado
   */
   void sendPTMFEventRemoveIDGL(String mensaje,ClusterGroupID clusterGroupID)
   {
    if (this.listaPTMFIDGLListeners.size() != 0)
    {
     ClusterNetEventGroup evento = new ClusterNetEventGroup(this,mensaje,clusterGroupID,false);

     Iterator iterator = this.listaPTMFIDGLListeners.listIterator();
     while(iterator.hasNext())
     {
        ClusterNetGroupListener ptmfListener = (ClusterNetGroupListener)iterator.next();
        ptmfListener.actionPTMFIDGL(evento);
     }
    }
   }

  //==========================================================================
  /**
   * Envía un evento ClusterNetEvent del tipo EVENTO_ADD_IDGL
   * con una cadena informativa.
   * @param mensaje Mensaje Informativo
   * @param id_socket Objeto ClusterGroupID que ha sido añadido al grupo
   */
   void sendPTMFEventAddID_Socket(String mensaje,ClusterMemberID id_socket)
   {
    if (this.listaPTMFID_SocketListeners.size() != 0)
    {
     ClusterNetEventMember evento = new ClusterNetEventMember(this,mensaje,id_socket,true);

     Iterator iterator = this.listaPTMFID_SocketListeners.listIterator();
     while(iterator.hasNext())
     {
        ClusterNetMemberListener ptmfListener = (ClusterNetMemberListener)iterator.next();
        ptmfListener.actionID_Socket(evento);
     }
    }
   }

  //==========================================================================
  /**
   * Envía un evento ClusterNetEvent del tipo EVENTO_REMOVE_ID_SOCKET
   * con una cadena informativa.
   * @param mensaje Mensaje Informativo
   * @param id_socket Objeto ClusterMemberID que ha sido eliminado
   */
   void sendPTMFEventRemoveID_Socket(String mensaje,ClusterMemberID id_socket)
   {
    if (this.listaPTMFID_SocketListeners.size() != 0)
    {
     ClusterNetEventMember evento = new ClusterNetEventMember(this,mensaje,id_socket,false);

     Iterator iterator = this.listaPTMFID_SocketListeners.listIterator();
     while(iterator.hasNext())
     {
        ClusterNetMemberListener ptmfListener = (ClusterNetMemberListener)iterator.next();
        ptmfListener.actionID_Socket(evento);
     }
    }
   }

   //==========================================================================
  /**
   * Envía un evento ClusterNetEvent del tipo EVENTO_DATOS_RECIBIDOS
   * con una cadena informativa.
   * @param mensaje Mensaje Informativo
   * @param id_socket Objeto ClusterMemberID que ha sido eliminado
   */
   void sendPTMFEventDatosRecibidos(String mensaje,int nBytes)
   {
    //ESTE EVENTO ES LANZADO DESDE COLARECEPCION. Y SOLAMENTE CUANDO
    //SE ESTA EN LOS MODOS NO FIABLE.

    if (this.listaPTMFDatosRecibidosListeners.size() != 0)
    {
     ClusterNetEventNewData evento = new ClusterNetEventNewData(this,mensaje,nBytes);

     Iterator iterator = this.listaPTMFDatosRecibidosListeners.listIterator();
     while(iterator.hasNext())
     {
        ClusterNetRxDataListener ptmfListener = (ClusterNetRxDataListener)iterator.next();
        ptmfListener.actionPTMFDatosRecibidos(evento);
     }
    }
   }

  //==========================================================================
  /**
   * Establece el tamaño del buffer de recepción
   * @param tamaño del buffer de recepción
   */
  void setCapacidadColaRecepcion(int size)
  {
    if(this.rxQueue!= null)
      this.rxQueue.setCapacidad(size);
  }

  //==========================================================================
  /**
   * Devuelve un boolean indicando si el callback ReceiveHandler de la
   * aplicación está activo o no.
   * @return un boolean
   */
  boolean isCallbackReceiveHandlerActivo()
  {
    if (this.receiveHandler != null)
     return false;
    else
     return true;
  }



  //==========================================================================
  /**
   * Un método para registrar un objeto que implementa la interfaz ClusterNetConnectionListener.
   * La interfaz ClusterNetConnectionListener se utiliza para notificar a las clases que se
   * registren de un evento ClusterNetEventConecction
   * @param obj El objeto Infohandler.
   * @return ClusterNetExcepcion Se lanza cuando ocurre un error al registrar el
   *  objeto callback
   */
  public void addPTMFConexionListener(ClusterNetConnectionListener obj)
  {
    this.listaPTMFConexionListeners.add(obj);
  }

  //==========================================================================
  /**
   * Elimina un objeto ClusterNetConnectionListener
   */
  public void removePTMFConexionListener(ClusterNetConnectionListener obj)
  {
    this.listaPTMFConexionListeners.remove(obj);
   }

  //==========================================================================
  /**
   * Un método para registrar un objeto que implementa la interfaz ClusterNetErrorListener.
   * La interfaz ClusterNetErrorListener se utiliza para notificar a las clases que se
   * registren de un evento ClusterNetEventError
   * @param obj El objeto ClusterNetErrorListener
   */
  public void addPTMFErrorListener(ClusterNetErrorListener obj)
  {
    this.listaPTMFErrorListeners.add(obj);
  }

  //==========================================================================
  /**
   * Elimina un objeto ClusterNetConnectionListener
   */
  public void removePTMFErrorListener(ClusterNetErrorListener obj)
  {
    this.listaPTMFErrorListeners.remove(obj);
  }




  //==========================================================================
  /**
   * Un método para registrar un objeto que implementa la interfaz ClusterNetGroupListener.
   * La interfaz ClusterNetGroupListener se utiliza para notificar a las clases que se
   * registren de un evento ClusterNetEventGroup
   * @param obj El objeto Indohandler.
   * @return ClusterNetExcepcion Se lanza cuando ocurre un error al registrar el
   *  objeto callback
   */
  public void addPTMFIDGLListener(ClusterNetGroupListener obj)
  {
    this.listaPTMFIDGLListeners.add(obj);
  }

  //==========================================================================
  /**
   * Elimina un objeto ClusterNetGroupListener
   * @param obj el objeto ClusterNetGroupListener a eliminar
   */
  public void removePTMFIDGLListener(ClusterNetGroupListener obj)
  {
    this.listaPTMFIDGLListeners.remove(obj);
  }



  //==========================================================================
  /**
   * Elimina un objeto ClusterNetMemberInputStreamListener
   * @param obj el objeto ID_SocketInputStreamListener a eliminar
   */
  public void removePTMFID_SocketInputStreamListener(ClusterNetMemberInputStreamListener obj)
  {
    if (this.rxQueue!=null)
     this.rxQueue.removePTMFID_SocketInputStreamListenerListener(obj);
  }





  /**
   * Un método para registrar un objeto que implementa la interfaz ClusterNetMemberListener.
   * La interfaz ClusterNetMemberListener se utiliza para notificar a las clases que se
   * registren de un evento ClusterNetEventGroup
   * @param obj El objeto Indohandler.
   * @return ClusterNetExcepcion Se lanza cuando ocurre un error al registrar el
   *  objeto callback
   */
  public void addPTMFID_SocketListener(ClusterNetMemberListener obj)
  {
    this.listaPTMFID_SocketListeners.add(obj);
  }

  //==========================================================================
  /**
   * Elimina un objeto ClusterNetMemberListener
   */
  public void removePTMFID_SocketListener(ClusterNetMemberListener obj)
  {
    this.listaPTMFID_SocketListeners.remove(obj);
  }

  //==========================================================================
  /**
   * Un método para registrar un objeto que implementa la interfaz ClusterNetRxDataListener.
   * La interfaz ClusterNetRxDataListener se utiliza para notificar a las clases que se
   * registren de un evento ClusterNetEventGroup
   * @param obj El objeto Indohandler.
   * @return ClusterNetExcepcion Se lanza cuando ocurre un error al registrar el
   *  objeto callback
   */
  public void addPTMFDatosRecibidosListener(ClusterNetRxDataListener obj)
  {
    //ESTE MÉTODO ES LLAMADO DESDE:
    // 1.- DatagramSocketPTMF.
    // 2.- ClusterNetInputStream. En este caso permite notificar simplemente de
    // la recepcion de datos, NO especifica en que ID_SocketINputStream.
    // Existe otro lado donde registrarse, en Id_SocketINputStream, en este caso
    // se notifica de la recepcion de informacion Multicast en ese flujo en
    // particular.
    this.listaPTMFDatosRecibidosListeners.add(obj);
  }

  //==========================================================================
  /**
   * Un método para registrar un objeto que implementa la interfaz ClusterNetMemberInputStreamListener.
   * La interfaz ClusterNetMemberInputStreamListener se utiliza para notificar a
   * las clases que se registren de un evento ClusterNetEventMemberInputStream
   * @param obj El objeto ClusterNetMemberInputStreamListener
   */
  public void addPTMFID_SocketInputStreamListener(ClusterNetMemberInputStreamListener obj)
  {
   if (this.rxQueue!=null)
    this.rxQueue.addPTMFID_SocketInputStreamListener (obj);
  }


  //==========================================================================
  /**
   * Elimina un objeto ClusterNetRxDataListener
   */
  public void removePTMFDatosRecibidosListener(ClusterNetRxDataListener obj)
  {
    this.listaPTMFIDGLListeners.remove(obj);
  }

  //==========================================================================
  /**
   * El método callback del canal. Implementación de la interfaz ChannelCallback.
   * Este método será llamado cuando se reciban datos en el Channel Multicast.
   * @param arg Un argumento para el método.
   * @param buf un buffer que contiene los datos recibidos.  El handler debe de
   * copiar los datos del buffer fuera de este objeto <b>sin modificar el
   * buffer. </b>
   * @param src Un objeto Address conteniendo la dirección fuente de los datos.
   * El handler debe de copiar la dirección fuera de este objeto <b>sin modificar
   * los datos.</b>
   */// Es usada por unicast y multicast
/*synchronized  public void canalCallback(int arg, Buffer buf, Address src)
  {
    final String  mn = "SocketClusterNetImp.canalCallback(int,Buffer,Address)";
    byte  tipo;

    //
    // Sincronizar threads.
    //
    //this.threadMutex.Lock();
    try
    {

    // ALEX: 27/04/2003
    // Tengo que reaactivar la entrada del loop para que en el caso de que halla un único socket
    // por grupo (clusterGroupID) el socket envíe los asentimientos correspondiente, es necesario por tanto
    // escuchar los TPDUS que se envían y procesarlos

    // DESCARTAR LOS PAQUETES ENVIADOS POR "NOZZOTROS", POR ESTE SOCKET, EN JAVA NO HAY
    // AÚN UN MECANISMO PARA DESHABILITAR EL LOOP DE LOS DATAGRAMAS MULTICAST
    // HACERLO EN JNI ES UN POCO "GUARRO".
    //
    //Log.debug(Log.SOCKET,mn,"Puerto Unicast recibido:"+(int)TPDU.getPuertoUnicast(buf)+" Puerto Unicast Local:"+(int)this.addressLocal.getPort());
    if( /*(this.cglThread.getEstadoCGL()!= ClusterNet.ESTADO_CGL_MONITOR)
      && */
/*    (this.addressLocal.getInetAddress().equals(src.getInetAddress()))
      && (TPDU.getPuertoUnicast(buf)==this.addressLocal.getPort()))
    {
 */     //Log.debug(Log.SOCKET,"SocketClusterNetImp.canalCallback","Datagrama DESCARTADO Emisor: "+src/*+"Buffer: "+buf*/);
      ////Log.debug(Log.SOCKET,"SocketClusterNetImp.canalCallback","ID_SOCKET recibido:"+(int)TPDU.getIdSocket(buf)+" ID_SOCKET:"+(int)this.ID_SOCKET);
/*      Log.debug(Log.CANAL_MULTICAST,"SocketClusterNetImp.canalCallback",".");
      return;
    }

  */  //Log.debug(Log.SOCKET,"SocketClusterNetImp.canalCallback","Arg: "+arg+" Emisor: "+src/*+"Buffer: "+buf*/);


      //
      // Determinar el tipo del TPDU y actuar conforme a ello.
      //
/*      switch (tipo = TPDU.getTipo(buf))
      {
        case ClusterNet.TPDU_CGL:
          //
          // Verificar el modo del socket
          //
          if(modo==ClusterNet.MODE_RELIABLE || modo==ClusterNet.MODE_MONITOR || modo == ClusterNet.MODE_DELAYED_RELIABLE)
            this.procesarTPDUCGL(buf,src);
          break;

        case ClusterNet.TPDU_DATOS:
         //
         // Verificar que el grupo está activo.
         //
          //if (!this.grupoMcastActivo
          //break;
          // indico si ha sido recibido por unicast
          this.procesarTPDUDatos(buf, src, (arg==1?true:false));
          break;

        default:
          Log.log(mn, "Tipo de TPDU recibido ("+tipo+") no válido.");
      }
    }
    catch(ClusterNetExcepcion e) {return;}
  }
*/
  //==========================================================================
  /**
   * Este método procesa un TPDU CGL.
   * @param buf un buffer que contiene los datos recibidos.  El handler debe de
   * copiar los datos del buffer fuera de este objeto <b>sin modificar el
   * buffer. </b>
   * @param src Un objeto Address conteniendo la dirección fuente de los datos.
   * El handler debe de copiar la dirección fuera de este objeto <b>sin modificar
   * los datos.</b>
   */
  synchronized private void procesarTPDUCGL(Buffer buf, Address src)
  {
    final String  mn          = "SocketClusterNetImp.procesarTPDUCGL";
    TPDUCGL       tpduCGL     = null;
    RegistroCGL   registroCGL = null;
    int           secuencia   = 0;

    try
    {


      if (buf==null || src==null || this.cglThread==null)
        return;
    //
    // Verificar redundancia...
    //
    secuencia=TPDUCGL.getN_Secuencia(buf);
    if(this.addressUltimoEmisor.equals(src) && ( secuencia == this.N_SECUENCIA_ULTIMO))
       return;
    else
    {   // ALEX: clonar ???
       this.addressUltimoEmisor = (Address) src.clone();
       this.N_SECUENCIA_ULTIMO = secuencia;
    }

    //
    // Parser el buffer. Construir TPDU CGL.
    //
    if (this.cglThread.getEstadoCGL() != ClusterNet.ESTADO_CGL_NULO )
    {
        tpduCGL = TPDUCGL.parseBuffer(buf);

        if((tpduCGL != null)&&(src!=null))
        {
          registroCGL = new RegistroCGL(tpduCGL,src);

          //
          // Añadir el TPDUCGL al Vector vectorRegistroCGL.
          // NOTA: La clase Vector es sincronizada.!!!!!!!
          //
          this.vectorRegistroCGL.addElement(registroCGL);

        }
    }

    }
    catch(ClusterNetExcepcion e)
    {
        return;
    }
    catch(ClusterNetInvalidParameterException e) {   return;  }

  }


  //==========================================================================
  /**
   * Los datos obtenidos por el socket no serán pasados al usuario
   * @exception ClusterNetExcepcion Se lanza si ocurre algún error cerrando el socket.
   */
  public void activarRecepcion() throws ClusterNetExcepcion
  {
   // Decir a la cola de recepción que no tiene que encolar más datos.
   this.rxQueue.setEncolarDatos (true);
  }

  //==========================================================================
  /**
   * Los datos obtenidos por el socket no serán pasados al usuario
   * @exception ClusterNetExcepcion Se lanza si ocurre algún error cerrando el socket.
   */
  public void desactivarRecepcion()
  {
   // Decir a la cola de recepción que no tiene que encolar más datos.
   if (this.rxQueue!=null)
     this.rxQueue.setEncolarDatos (false);
  }



 //==========================================================================
  /**
   * Este método procesa un TPDU Datos.
   * @param buf un buffer que contiene los datos recibidos.  El handler debe de
   * copiar los datos del buffer fuera de este objeto <b>sin modificar el
   * buffer. </b>
   * @param src Un objeto Address conteniendo la dirección fuente de los datos.
   * El handler debe de copiar la dirección fuera de este objeto <b>sin modificar
   * los datos.</b>
   */
  synchronized  private void procesarTPDUDatos(Buffer buf, Address src,boolean recibidoPorUnicast)
  {
    final String  mn = "SocketClusterNetImp.procesarTPDUDatos(Buffer,Address)";
    byte  subtipo;
    int   offset = 0;

    if(this.dataThread == null)
      return;

    IPv4 ipv4Emisor = src.toIPv4 ();
    //Log.debug (Log.SOCKET,mn,"");



    //
    // Determinar el subtipo del TPDU CGL que se ha recibido.
    //

    try
    {
      subtipo = (byte)TPDUDatos.getSubtipo (buf);
    }
    catch(ClusterNetExcepcion e)
    {
      //
      // Las excepciones de ClusterNet ya reportan el mensaje informativo por defecto
      //

      return;
    }

    switch (subtipo)
    {
     case ClusterNet.SUBTIPO_TPDU_DATOS_NORMAL:
      try {
       TPDUDatosNormal tpduDatosNormal = TPDUDatosNormal.parserBuffer (buf,ipv4Emisor);
       tpduDatosNormal.recibidoPorUnicast = recibidoPorUnicast;
       if (tpduDatosNormal!=null)
          this.vectorRegistroDatos.add (tpduDatosNormal);
      }catch (ClusterNetExcepcion e) {return;}
       catch (ClusterNetInvalidParameterException e) {return;}
      break;

     case ClusterNet.SUBTIPO_TPDU_DATOS_RTX:
      try {
       TPDUDatosRtx tpduDatosRtx = TPDUDatosRtx.parserBuffer (buf,ipv4Emisor);
       tpduDatosRtx.recibidoPorUnicast = recibidoPorUnicast;
       if (tpduDatosRtx!=null)
          this.vectorRegistroDatos.add (tpduDatosRtx);
      }catch (ClusterNetExcepcion e) {return;}
       catch (ClusterNetInvalidParameterException e) {return;}
      break;

     case ClusterNet.SUBTIPO_TPDU_DATOS_ACK:
      try {
       TPDUACK tpduACK = TPDUACK.parserBuffer (buf,ipv4Emisor);
       tpduACK.recibidoPorUnicast = recibidoPorUnicast;
       if (tpduACK!=null)
          this.vectorRegistroDatos.add (tpduACK);
      }catch (ClusterNetExcepcion e) {return;}
       catch (ClusterNetInvalidParameterException e) {return;}
      break;

     case ClusterNet.SUBTIPO_TPDU_DATOS_HACK:
      try {
       TPDUHACK tpduHACK = TPDUHACK.parserBuffer (buf,ipv4Emisor);
       tpduHACK.recibidoPorUnicast = recibidoPorUnicast;
       if (tpduHACK!=null)
          this.vectorRegistroDatos.add (tpduHACK);
      }catch (ClusterNetExcepcion e) {return;}
       catch (ClusterNetInvalidParameterException e) {return;}
      break;

     case ClusterNet.SUBTIPO_TPDU_DATOS_HSACK:
      try {
       TPDUHSACK tpduHSACK = TPDUHSACK.parserBuffer (buf,ipv4Emisor);
       tpduHSACK.recibidoPorUnicast = recibidoPorUnicast;
       if (tpduHSACK!=null)
          this.vectorRegistroDatos.add (tpduHSACK);
      }catch (ClusterNetExcepcion e) {return;}
       catch (ClusterNetInvalidParameterException e) {return;}
      break;

     case ClusterNet.SUBTIPO_TPDU_DATOS_MACK:
      try {
       TPDUMACK tpduMACK = TPDUMACK.parserBuffer (buf,ipv4Emisor);
       tpduMACK.recibidoPorUnicast = recibidoPorUnicast;
       if (tpduMACK!=null)
          this.vectorRegistroDatos.add (tpduMACK);
      }catch (ClusterNetExcepcion e) {return;}
       catch (ClusterNetInvalidParameterException e) {return;}
      break;

     case ClusterNet.SUBTIPO_TPDU_DATOS_NACK:
      try {
       TPDUNACK tpduNACK = TPDUNACK.parserBuffer (buf,ipv4Emisor);
       tpduNACK.recibidoPorUnicast = recibidoPorUnicast;
       if (tpduNACK!=null)
          this.vectorRegistroDatos.add (tpduNACK);
      }catch (ClusterNetExcepcion e) {return;}
       catch (ClusterNetInvalidParameterException e) {return;}
      break;

     case ClusterNet.SUBTIPO_TPDU_DATOS_HNACK:
      try {
       TPDUHNACK tpduHNACK = TPDUHNACK.parserBuffer (buf,ipv4Emisor);
       tpduHNACK.recibidoPorUnicast = recibidoPorUnicast;
       if (tpduHNACK!=null)
          this.vectorRegistroDatos.add (tpduHNACK);
      }catch (ClusterNetExcepcion e) {return;}
       catch (ClusterNetInvalidParameterException e) {return;}
      break;

     default:
       System.out.println ("Subtipo del TPDU (paquete) Datos no válido. TPDU no procesado.");
       return;
    } // Fin del switch

  }

  //==========================================================================
  /**
   * Este método inicia la búsqueda del grupo_local.<br>
   * Si el grupo_local no se encuentra o no se puede unir a ninguno
   * se crea uno nuevo
   */
  private void joinGrupoLocal()
  {
    this.cglThread.setEstadoCGL(ClusterNet.ESTADO_CGL_BUSCAR_GL);

    //
    // Bloquear hasta que se enlace al grupo multicast
    // ... ME LIBERA CGLThread cuando nos unamos al grupo local o se cree uno nuevo.
    //
    //Log.debug(Log.SOCKET,"SocketClusterNetImp.joingrupolocal","Antes del semáforo...");
    this.semaforoAplicacion.down();

  }

  //==========================================================================
  /**
   * Devuelve el Modo del Socket
   *
   */
   int getModo() { return this.modo;}


  //==========================================================================
  /**
   * Establece el tiempo de espera máximo que el thread de usuario espera
   * en una llamada al método receive() sin que hallan llegado datos.
   * @param iTiempo Tiempo máximo de espera en mseg. 0 espera infinita.
   */
  void setSoTimeOut(int iTiempo)
  {
     this.rxQueue.setTimeOut(iTiempo);
  }

  //==========================================================================
  /**
   * Establece el tiempo de espera máximo que el thread de usuario espera
   * en una llamada al método receive() sin que hallan llegado datos.
   * @param iTiempo Tiempo máximo de espera en mseg. 0 espera infinita.
   */
  int getSoTimeOut()
  {
     return this.rxQueue.getTimeOut();
  }


  //==========================================================================
  /**
   * Devuelve un TreeMap con los ID_Sockets del Grupo Local
   * @return TreeMap con los ID_Sockets del Grupo Local.
   */
   TreeMap getID_Sockets()
   {
     if (this.modo == ClusterNet.MODE_RELIABLE || this.modo == ClusterNet.MODE_DELAYED_RELIABLE)
     {
       if (this.cglThread != null)
         return this.cglThread.getID_Sockets();
       return null;
     }
     else
       return null;

   }

  //==========================================================================
  /**
   * Devuelve un TreeMap con los IDGLs a los que alcanzamos
   * @return TreeMap con los IDGLs a los que alcanzamos
   */
   TreeMap getIDGLs()
   {
     if (this.modo == ClusterNet.MODE_RELIABLE || this.modo == ClusterNet.MODE_DELAYED_RELIABLE)
     {
      if (this.cglThread != null)
         return this.cglThread.getIDGLs();
      return null;
     }
     else
       return null;

   }

  //==========================================================================
  /**
   * Obtener el numero de IDGLs
   * @return int
   */
   int getNumeroIDGLs()
   {
     if ((this.cglThread != null) &&(this.modo == ClusterNet.MODE_RELIABLE || this.modo == ClusterNet.MODE_DELAYED_RELIABLE))
     {
        return this.cglThread.getNumeroIDGLs();
     }
     else
      return 0;
   }

  //==========================================================================
  /**
   * Obtener el numero de IDSockets
   * @return int
   */
   int getNumeroID_Sockets()
   {
     if ((this.cglThread != null) &&(this.modo == ClusterNet.MODE_RELIABLE || this.modo == ClusterNet.MODE_DELAYED_RELIABLE))
     {
        return this.cglThread.getNumeroID_Sockets();
     }
     else
      return 0;
   }



  //==========================================================================
  /**
   * ERROR. ESTE MÉTODO SE LLAMA SI OCURRE UN ERROR GRAVE EN LA CLASE (Socket
   * Cerrado, etc.).<br>
   * Establece la variable error a true y para todos los hilos del proceso y
   * libera todos los recursos asociados.
   */
  void error(IOException e)
  {
    String s = null;

    this.error = true;

    //
    // Cerrar Socket
    //
    try
    {
     this.closePTMF(ClusterNet.CLOSE_INMEDIATE/* NO ESTABLE*/);
    }catch(ClusterNetExcepcion ie){; }

    if(e!= null)
    {
      System.out.println ("ERROR: " + e.getMessage()+"\nTraza de pila:\n");
      Log.log("ERROR: ",e.getMessage()+"\nTraza de pila:\n");
      e.printStackTrace();
    }

    Log.exit(-1);
  }


   //==========================================================================
   /**
    * Devuelve una cadena informativa.
    */
   public String toString ()
   {
    return "Dir. Interfaz: " + this.addressInterfaz +
           "\nDir. Local: " + this.addressLocal +
           "\nDir. Multicast: " + this.canalMcast.getAddressMulticast() +
           "\nDir. Unicast: " + this.unicastChannel.getAddressUnicast()
           ;
   }



//=============================================================================

/**

 * Actualiza el ratio de envío de datos de usuario, el cual especifica la

 * máxima velocidad de envío de nuevos datos de usuario al grupo multicast.

 * Comienza a considerar el ratio a partir de este momento.

 * @param bytes_x_seg bytes por segundo.

 * @return el valor al que se ha actualizado el ratio de datos de usuario en bytes

 * por segundo

 */

 public long setRatioUsuario (long bytes_x_seg)

 {
   if(this.dataThread!= null)
    return this.dataThread.setRatioUsuario(bytes_x_seg);

   return 0;
 }


}










