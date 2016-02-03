//============================================================================
//
//	Copyright (c) 1999-2015 . All Rights Reserved.
//
//----------------------------------------------------------------------------
//
//	Fichero: ClusterNet.java  1.0 24/09/99
//
//
//	Descripción: Clase ClusterNet.
//
//  Historial: 
//	14/10/2014 Change Licence to LGPL
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.TreeMap;


/**
 * <STRONG><b>
 * Class ClusterNet.<br>
 *
 * This class provide a "Cluster Socket" M</b>
 * The cluster socket provide a input and output stream to be easily used by applications to implement cluster applications, 
 * where multiple communication way between peers nodes are needed.
 *    
 *
 * The implementation is provide by the class SocketClusterNetImp.<br>
 *
 * This class is Not thread safe.
 * @version  1.0
 * @author M. Alejandro García Domínguez
 * <A HREF="mailto:alejandro.garcia.dominguez@gmail.com">(alejandro.garcia.dominguez@gmail.com)</A><p>
 *			   Antonio Berrocal Piris (antonioberrocalpiris@gmail.com)
 */

public class ClusterNet {


	  //==========================================================================
	  //
	  //  GLOBAL STATIC VARS
	  //

	  /** Modo FIABLE del protocolo ClusterNet  */
	  public static final int MODE_RELIABLE      = 0x1000;
	  /** Modo NO_FIABLE del protocolo ClusterNet  */
	  public static final int MODE_NO_RELIABLE   = 0x1001;
	  /** Modo MONITOR del protocolo ClusterNet  */
	  public static final int MODE_MONITOR     = 0x1002;
	  /** Modo FIABLE_RETRASADO del protocolo ClusterNet  */
	  public static final int MODE_DELAYED_RELIABLE = 0x1004;
	  /** Modo NO_FIABLE_ORDENADO del protocolo ClusterNet  */
	  public static final int MODE_NO_RELIABLE_ORDERED = 0x1008;

	  /** Cierre estable de la conexion Multicast  */
	  public static final boolean CLOSE_STABLE   = true;
	  /** Cierre Immediato de la conexion Multicast  */
	  public static final boolean CLOSE_INMEDIATE = false;


	  /** Número de versión de ClusterNet. */
	  public static final int VERSION = 1;


	  /**
	   * Tamaño máximo (en bytes) que puede tener un TPDU. 63Kb, reservamos 1Kb para
	   * las Cabeceras del Nivel de Transporte y del Nivel de Red (IP); el tamaño
	   * máxio un datagrama IP es 64Kb, incluye datos y cabecera IP.
	   */
	  public static final int TPDU_MAX_SIZE = 1024 *63;
	 /**
	   * Número máximo de bytes que pueden viajar en un TPDU de datos. Hay que reservar
	   * suficientes bytes para enviar identificadores de clusterGroupID o id_socket que no han
	   * enviado asentimiento, para el caso en el que necesite ser rtx. Al menos deberá
	   * caber un identificador clusterGroupID o id_socket (el más grande de los dos).
	   * Reservamos para 20 identificadores: 20 * 6 = 120 bytes
	   */
	  public static final int TPDU_MAX_SIZE_PAYLOAD = TPDU_MAX_SIZE
	                                                  - TPDUDatosNormal.LONGHEADER;

	  /**
	   * MTU (Maximum Transfer Unit). EL MTU NO PUEDE SER SUPERIOR AL TPDU_MAX_SIZE.
	   */
	   public static final int MTU = 1024 * 1; //2


	  /**
	   * TAMAÑO VENTANA DE EMISIÓN. Nº de TPDUs que caben en la ventana.
	   */
	  public static final int TAMAÑO_VENTANA_EMISION   = 24;//24;

	  /**
	   * TAMAÑO VENTANA DE RECEPCIÓN. Nº de TPDUs que caben en la ventana.
	   */
	  public static final int TAMAÑO_VENTANA_RECEPCION = 24;//24;


	  /** Tamaño por defecto de la Cola de Emisión en bytes*/
	  public static final int TAMAÑO_DEFECTO_COLA_EMISION   = 1024 * 60; //64kB

	  /** Tamaño por defecto de la Cola de Recepción en bytes*/
	  public static final int TAMAÑO_DEFECTO_COLA_RECEPCION = 1024 * 300; //300kB

	 /**
	  * Tamaño del buffer de emision del socket en bytes.
	  * En algunos S.O. como W'95 existe una limitacion de 64Kb
	  */
	  public static final int SIZE_BUFFER_SOCKET_EMISION = 1024 * 64;//1024 * 100;

	 /**
	  * Tamaño del buffer de recepcion del socket en bytes.
	  * En algunos S.O. como W'95 existe una limitacion de 64Kb
	  */
	  public static final int SIZE_BUFFER_SOCKET_RECEPCION = 1024 * 64;//1024 * 300;

	/**
	 * Numero de TPDUs que DataThread puede procesar consecutivamente.
	 */
	  public static final int MAX_TPDU_PROCESAR_CONSECUTIVAMENTE = (TAMAÑO_VENTANA_EMISION/2) +1;

	  /** TPDU Tipo CGL */
	  public static final int TPDU_CGL      = 0;
	  /** TPDU Tipo DATOS */
	  public static final int TPDU_DATOS    = 1;

	  /** Subtipos TPDU DATOS NORMAL*/
	  public static final byte SUBTIPO_TPDU_DATOS_NORMAL     = 0x00; // 000
	  /** Subtipos TPDU DATOS RTX*/
	  public static final byte SUBTIPO_TPDU_DATOS_RTX        = 0x01; // 001
	  /** Subtipos TPDU DATOS MACK*/
	  public static final byte SUBTIPO_TPDU_DATOS_MACK       = 0x02; // 010
	  /** Subtipos TPDU DATOS ACK*/
	  public static final byte SUBTIPO_TPDU_DATOS_ACK        = 0x03; // 011
	  /** Subtipos TPDU DATOS HACK*/
	  public static final byte SUBTIPO_TPDU_DATOS_HACK       = 0x04; // 101
	  /** Subtipos TPDU DATOS HSACK*/
	  public static final byte SUBTIPO_TPDU_DATOS_HSACK      = 0x05; // 101
	  /** Subtipos TPDU DATOS NACK*/
	  public static final byte SUBTIPO_TPDU_DATOS_NACK       = 0x06; // 110
	  /** Subtipos TPDU DATOS HNACK*/
	  public static final byte SUBTIPO_TPDU_DATOS_HNACK      = 0x07; // 111

	  /** Ambito de TTL de host */
	  public static final int AMBITO_HOST          = 0;
	  /** Ambito de TTL de Subred */
	  public static final int AMBITO_SUBRED        = 1;
	  /** Ambito de TTL de Local */
	  public static final int AMBITO_LOCAL         = 8;
	  /** Ambito de TTL de Regional */
	  public static final int AMBITO_REGIONAL      = 32;
	  /** Ambito de TTL de Europea */
	  public static final int AMBITO_EUROPEA       = 64;
	  /** Ambito de TTL de Internacional */
	  public static final int AMBITO_INTERNACIONAL = 128;
	  /** Ambito de TTL de Inrestringido */
	  public static final int AMBITO_INRESTRINGIDO = 255;


	  /**Subtipo TPDU CGL BUSCAR_GRUPO_LOCAL */
	  public static final byte TPDU_CGL_BUSCAR_GRUPO_LOCAL        = 1;
	  /**Subtipo TPDU CGL GRUPO_LOCAL */
	  public static final byte TPDU_CGL_GRUPO_LOCAL               = 2;
	  /**Subtipo TPDU CGL UNIRSE_A_GRUPO_LOCAL */
	  public static final byte TPDU_CGL_UNIRSE_A_GRUPO_LOCAL      = 3;
	  /**Subtipo TPDU CGL SCOKET_ACEPTADO_EN_GRUPO_LOCAL */
	  public static final byte TPDU_CGL_SOCKET_ACEPTADO_EN_GRUPO_LOCAL = 4;
	  /**Subtipo TPDU CGL DEJAR_GRUPO_LOCAL */
	  public static final byte TPDU_CGL_DEJAR_GRUPO_LOCAL         = 5;
	  /**Subtipo TPDU CGL ELIMINACION_GRUPO_LOCAL */
	  public static final byte TPDU_CGL_ELIMINACION_GRUPO_LOCAL   = 6;
	  /**Subtipo TPDU CGL BUSCAR_GRUPO_LOCAL_VECINO */
	  public static final byte TPDU_CGL_BUSCAR_GRUPO_LOCAL_VECINO = 7;
	  /**Subtipo TPDU CGL GRUPO_LOCAL_VECINO */
	  public static final byte TPDU_CGL_GRUPO_LOCAL_VECINO        = 8;
	  /**Subtipo TPDU CGL BUSCAR_GL_PARA_EMISOR */
	  public static final byte TPDU_CGL_BUSCAR_GL_PARA_EMISOR     = 9;
	  /**Subtipo TPDU CGL GL_PARA_EMISOR */
	  public static final byte TPDU_CGL_GL_PARA_EMISOR            = 10;

	  /** Estado de la máquina CGL: BUSCAR_GL  */
	  public static final int ESTADO_CGL_BUSCAR_GL               = 0x100;
	  /** Estado de la máquina CGL: ESPERAR_ACEPTACION_GL  */
	  public static final int ESTADO_CGL_ESPERAR_ACEPTACION_GL   = 0x103;
	  /** Estado de la máquina CGL: MIEMBRO_GL  */
	  public static final int ESTADO_CGL_MIEMBRO_GL              = 0x104;
	  /** Estado de la máquina CGL: CREAR_GL  */
	  public static final int ESTADO_CGL_CREAR_GL                = 0x105;
	  /** Estado de la máquina CGL: BUSCAR_GL_VECINOS  */
	  public static final int ESTADO_CGL_BUSCAR_GL_VECINOS       = 0x106;
	  /** Estado de la máquina CGL: ESTADO_CGL_DEJAR_GL  */
	  public static final int ESTADO_CGL_DEJAR_GL                = 0x107;
	  /** Estado de la máquina CGL: ESTADO_CGL_MONITOR  */
	  public static final int ESTADO_CGL_MONITOR                 = 0x110;
	  /** Estado de la máquina CGL: ESTADO_CGL_NULO  */
	  public static final int ESTADO_CGL_NULO                    = 0x111;

	  /** Número máximo de sockets en el Grupo Local (GL) */
	  public static final int MAX_SOCKETS_GL     =   24;


	  /** RTT  */
	  public static final int RTT = 3000; //10000;  // 1 Segundos

	  /** OPORTUNIDADES_RTT  */
	  public static final int OPORTUNIDADES_RTT = 3; // Número de intentos

	  /** Tiempo entre envios de cada TPDU  */
	  public static final int T_MIN_ENTRE_ENVIOS = 0;

	  /** Tiempo base que se usa para calcular los tiempos aleatorios.*/
	  public static final int T_BASE = 200; // 0.2 Segundos

	  /** Máximo tiempo aleatorio para enviar un MACK. Tiene que ser mayor que T_BASE*/
	  public static final int MAX_TRANDOM_MACK = 800; // 0.8 Segundos

	  /** Máximo tiempo aleatoriO para enviar un ASENT_NEG. Tiene que ser mayor que T_BASE*/
	  public static final int MAX_TRANDOM_ASENT_NEG = 2000; // 2 Segundos



	  /** Número de TPDU INICIALES por ráfaga */
	  public static final int TPDUS_POR_RAFAGA  = 1000; //100000 ;//ClusterNet.TAMAÑO_VENTANA_EMISION;

	  /** Petición de ACK INICIALES cada X TPDU. */
	  public static final int ACKS_CADA_X_TPDUS = (TAMAÑO_VENTANA_EMISION * 4)/5;

	  /**
	   * Tiempo máximo desde que se inserta un TPDU sin el bit ACK activado en
	   * la ventana de emisión y uno con el bit ACK activado.
	   */
	  public static final long T_MAX_TPDU_SIN_ACK = RTT *1;//  / 2;

	 /**
	  * Ratio de emision de datos de usuario por defecto. En bytes por segundo.
	  */
	  public static final long RATIO_USUARIO_BYTES_X_SEG  = 1024*1024*100;

	 /**
	  * Tiempo máximo que puede emplear un socket o clusterGroupID nuevo en sincronizarse
	  * con los emisores.
	  */
	  public static final long TIEMPO_MAXIMO_SINCRONIZACION = RTT*OPORTUNIDADES_RTT + T_MAX_TPDU_SIN_ACK;


	  /**
	   * Tiempo en mseg que mide los intervalos para la comprobación de la
	   * inactividad de emisores.
	   */
	  public static final long TIEMPO_COMPROBACION_INACTIVIDAD_EMISORES = 60000; // 60 seg.


	  /**
	   * Tiempo de inactividad máximo durante el cual un emisor puede estar inactivo.
	   */
	  public static final long TIEMPO_MAX_INACTIVIDAD_EMISOR = 60000 * 10; // 10 minutos
	
	
  /**
   * SocketClusterNetImp, clase que implementa el API presentada en esta clase.
   */
  private SocketClusterNetImp socketClusterNetImp = null;

  //==========================================================================
  /**
   * Constructor genérico.
   * Crea un Socket ClusterNet en el modo FIABLE y añade el emisor a un grupo Multicast,
   * cerrando primero cualquier grupo activo.
   * Usa la interfaz por defecto del sistema para enviar
   * y recibir los datos.<br>
   * Solo un grupo multicast puede estar activo en un determinado momento.
   * @param grupoMulticast Un objeto Address con la dirección del grupo
   *  multicast al que se unirá (join) este emisor y con el puerto ClusterNet al que
   *  se enviarán/recibirán los datos multicast.
   * @param ttl El valor del TTL usado para todos los paquetes multicast.
   * <ul>
   * <il>ttl 1   = Subred
   * <il>ttl 8   = Local
   * <il>ttl 32  = Regional
   * <il>ttl 48  = Nacional
   * <il>ttl 64  = Europea
   * <il>ttl 128 = Internacional
   * </ul>
   * @exception ClusterNetInvalidParameterException Se lanza si el parámetro modo
   *  no es correcto
   * @exception ClusterNetExcepcion Se lanza si ocurre un error.
    * @see ClusterNet
   */
 /* public ClusterNet(Address grupoMulticast, byte ttl) throws ClusterNetInvalidParameterException, ClusterNetExcepcion
  {
    socketClusterNetImp = new SocketClusterNetImp(grupoMulticast,null,ttl,MODE_RELIABLE,null,null,null);
  }*/

  //==========================================================================
  /**
   * Constructor genérico.
   * Crea un Socket ClusterNet sin especificar el modo del socket. El modo del Socket
   * solo puede ser MODE_RELIABLE ó MODE_SEMIRELIABLE.
   * Se añade el emisor a un grupo Multicast,
   * cerrando primero cualquier grupo activo.
   * Usa la interfaz por defecto del sistema para enviar
   * y recibir los datos o null<br>
   * Solo un grupo multicast puede estar activo en un determinado momento.
   * @param grupoMulticast Un objeto Address con la dirección del grupo
   *  multicast al que se unirá (join) este emisor y con el puerto ClusterNet al que
   *  se enviarán/recibirán los datos multicast.
   * @param ttl El valor del TTL usado para todos los paquetes multicast.
   * <ul>
   * <il>ttl 1   = Subred
   * <il>ttl 8   = Local
   * <il>ttl 32  = Regional
   * <il>ttl 48  = Nacional
   * <il>ttl 64  = Europea
   * <il>ttl 128 = Internacional
   * </ul>
   * @param Modo de creación del ClusterNet
   * @param cipher Objeto javax.crypto.Cipher utilizado para codificar o null
   * si no se quiere codificar
   * @param unCipher Objeto javax.crypto.Cipher utilizado para descodificar o
   * null se no se quiere descodificar.
   * @exception ClusterNetInvalidParameterException Se lanza si el parámetro modo
   *  no es correcto
   * @exception ClusterNetExcepcion Se lanza si ocurre un error.
    * @see ClusterNet
   */
  public ClusterNet(Address grupoMulticast,Address interfaz, byte ttl, int modo
  ,ClusterNetConnectionListener listener,
   javax.crypto.Cipher cipher, javax.crypto.Cipher unCipher) throws ClusterNetInvalidParameterException, ClusterNetExcepcion
  {
    switch(modo)
    {
     case MODE_RELIABLE:
     case MODE_DELAYED_RELIABLE:
       socketClusterNetImp = new SocketClusterNetImp(grupoMulticast,interfaz,ttl,modo,listener,cipher,unCipher);
       break;
     default:
      throw new ClusterNetInvalidParameterException("MODO de creación del ClusterNet erroneo.");
    }
  }
    //==========================================================================
  /**
   * Constructor genérico.
   * Crea un Socket ClusterNet sin especificar el modo del socket. El modo del Socket
   * solo puede ser MODE_RELIABLE ó MODE_SEMIRELIABLE.
   * Se añade el emisor a un grupo Multicast,
   * cerrando primero cualquier grupo activo.
   * Usa la interfaz por defecto del sistema para enviar
   * y recibir los datos.<br>
   * Solo un grupo multicast puede estar activo en un determinado momento.
   * @param grupoMulticast Un objeto Address con la dirección del grupo
   *  multicast al que se unirá (join) este emisor y con el puerto ClusterNet al que
   *  se enviarán/recibirán los datos multicast.
   * @param ttl El valor del TTL usado para todos los paquetes multicast.
   * <ul>
   * <il>ttl 1   = Subred
   * <il>ttl 8   = Local
   * <il>ttl 32  = Regional
   * <il>ttl 48  = Nacional
   * <il>ttl 64  = Europea
   * <il>ttl 128 = Internacional
   * </ul>
   * @param Modo de creación del ClusterNet
   * @exception ClusterNetInvalidParameterException Se lanza si el parámetro modo
   *  no es correcto
   * @exception ClusterNetExcepcion Se lanza si ocurre un error.
    * @see ClusterNet
   */
  public ClusterNet(Address grupoMulticast,Address interfaz, byte ttl, int modo) throws ClusterNetInvalidParameterException, ClusterNetExcepcion
  {
    switch(modo)
    {
     case MODE_RELIABLE:
     case MODE_DELAYED_RELIABLE:
       socketClusterNetImp = new SocketClusterNetImp(grupoMulticast,interfaz,ttl,modo,null,null,null);
       break;
     default:
      throw new ClusterNetInvalidParameterException("MODO de creación del ClusterNet erroneo.");
    }
  }

    //==========================================================================
  /**
   * Constructor genérico.
   * Crea un Socket ClusterNet sin especificar el modo del socket. El modo del Socket
   * solo puede ser MODE_RELIABLE ó MODE_SEMIRELIABLE.
   * Se añade el emisor a un grupo Multicast,
   * cerrando primero cualquier grupo activo.
   * Usa la interfaz por defecto del sistema para enviar
   * y recibir los datos.<br>
   * Solo un grupo multicast puede estar activo en un determinado momento.
   * @param grupoMulticast Un objeto Address con la dirección del grupo
   *  multicast al que se unirá (join) este emisor y con el puerto ClusterNet al que
   *  se enviarán/recibirán los datos multicast.
   * @param ttl El valor del TTL usado para todos los paquetes multicast.
   * <ul>
   * <il>ttl 1   = Subred
   * <il>ttl 8   = Local
   * <il>ttl 32  = Regional
   * <il>ttl 48  = Nacional
   * <il>ttl 64  = Europea
   * <il>ttl 128 = Internacional
   * </ul>
   * @param Modo de creación del ClusterNet
   * @param ClusterNetConnectionListener listener
   * @exception ClusterNetInvalidParameterException Se lanza si el parámetro modo
   *  no es correcto
   * @exception ClusterNetExcepcion Se lanza si ocurre un error.
    * @see ClusterNet
   */
  public ClusterNet(Address grupoMulticast,Address interfaz, byte ttl, int modo, ClusterNetConnectionListener listener) throws ClusterNetInvalidParameterException, ClusterNetExcepcion
  {
    switch(modo)
    {
     case MODE_RELIABLE:
     case MODE_DELAYED_RELIABLE:
       socketClusterNetImp = new SocketClusterNetImp(grupoMulticast,interfaz,ttl,modo,listener,null,null);
       break;
     default:
      throw new ClusterNetInvalidParameterException("MODO de creación del ClusterNet erroneo.");
    }
  }

  //==========================================================================
  /**
   * Este constructor abre el socket y añade el emisor a un grupo Multicast,
   * cerrando primero cualquier grupo activo. Usa la interfaz que se especifica
   * para enviar y recibir los datos.<br>
   * Solo un grupo multicast puede estar activo en un determinado momento.
   * @param grupoMulticast Un objeto Address con la dirección del grupo
   *  multicast al que se unirá (join) este emisor y con el puerto ClusterNet al que
   *  se enviarán/recibirán los datos multicast.
   * @param interfaz Interfaz por la que se envian/reciben los datos.
   * @param ttl El valor del TTL usado para todos los paquetes multicast.<br>
   * <ul>
   * <il>ttl 1   = Subred
   * <il>ttl 8   = Local
   * <il>ttl 32  = Regional
   * <il>ttl 48  = Nacional
   * <il>ttl 64  = Europea
   * <il>ttl 128 = Internacional
   * </ul>
   * @exception ClusterNetInvalidParameterException Se lanza si el parámetro modo
   *  no es correcto
   * @exception ClusterNetExcepcion Se lanza si ocurre un error.
    * @see ClusterNet
   */
/*  public ClusterNet(Address grupoMulticast, Address interfaz, byte ttl) throws ClusterNetInvalidParameterException, ClusterNetExcepcion
  {
    socketClusterNetImp = new SocketClusterNetImp(grupoMulticast,interfaz,ttl,MODE_RELIABLE,null,null,null);
  }*/

  //==========================================================================
  /**
   * Cierra el socket. Quita al emisor (leave) del grupo multicast activo,
   * cualquier dato recibido del grupo multiast se perderá.<br>
   * Sólo un grupo multicast puede estar activo en cualquier momento.
   * @param bOrdenado Boolean que indica el modo de cierre. <br>
   * si el valor es true el socket realiza una desconexion ordenada (RECOMENDADO),
   * si es false realiza una desconexion abrupta.
   * @exception ClusterNetExcepcion Se lanza si ocurre algún error cerrando el socket.
   */
  public void close(boolean bOrdenada) throws ClusterNetExcepcion
  {
    socketClusterNetImp.closePTMF(bOrdenada);
  }

  //==========================================================================
  /**
   * Una vez llamada esta función no se podrá enviar ningún byte más.
   */
  public void endTx() throws ClusterNetExcepcion
  {
    socketClusterNetImp.closePTMFEmision();
  }

  //==========================================================================
  /**
   * Cierra el socket. Quita al emisor (leave) del grupo multicast activo,
   * cualquier dato recibido del grupo multiast se perderá.<br>
   * Sólo un grupo multicast puede estar activo en cualquier momento.
   * @exception ClusterNetExcepcion Se lanza si ocurre algún error cerrando el socket.
   */
  public void close() throws ClusterNetExcepcion
  {
    socketClusterNetImp.closePTMF(true);
  }



  //==========================================================================
  /**
   * Los datos obtenidos por el socket son pasados al usuario
   * @exception ClusterNetExcepcion Se lanza si ocurre algún error cerrando el socket.
   */
  public void enableRx() throws ClusterNetExcepcion
  {
    socketClusterNetImp.activarRecepcion();
  }

  //==========================================================================
  /**
   * Los datos obtenidos por el socket no serán pasados al usuario
   * @exception ClusterNetExcepcion Se lanza si ocurre algún error cerrando el socket.
   */
  public void disableRx() throws ClusterNetExcepcion
  {
    socketClusterNetImp.desactivarRecepcion();
  }


  //==========================================================================
  /**
   * Return a objet ClusterNetInputStream, a input stream for this cluster socket.
   * @return ClusterNetInputStream , to read bytes from this cluster socker.
   * @exception IOException throw if the socket can't create the stream.
   */
  public ClusterNetInputStream getClusterInputStream() throws IOException
  {
    return socketClusterNetImp.getMulticastInputStream();
  }

  //==========================================================================
  /**
   * Return a objet ClusterNetOutputStream, a output stream for this cluster socket.
   * @return ClusterNetOutputStream , to write bytes to this cluster socker.
   * @exception IOException throw if the socket can't create the stream.
   */
  public ClusterNetOutputStream getClusterOutputStream() throws IOException
  {
    return socketClusterNetImp.getMulticastOutputStream();
  }
  
  
  //==========================================================================
  /**
   * Return a objet MemberInputStream, a input stream for a given ClusterMemberID
   * @return MemberInputStream , to read bytes from the member given.
   * @exception IOException throw if the socket can't create the stream.
   */
  public MemberInputStream getMemberInputStream(ClusterMemberID id) throws IOException
  {
    return null;
    //TODO 
  }

  //==========================================================================
  /**
   * Return a objet MemberOutputStream, a output stream for a given ClusterMemberID
   * @return MemberOutputStream , to write bytes to the member given.
   * @exception IOException throw if the socket can't create the stream.
   */
  public MemberOutputStream getMemberOutputStream(ClusterMemberID id) throws IOException
  {
    return null;
    //TODO 
  }
  
  

  //==========================================================================
  /**
   * Un método para registrar un objeto que implementa la interfaz ClusterNetConnectionListener.
   * La interfaz ClusterNetConnectionListener se utiliza para notificar a las clases que se
   * registren de un evento ClusterNetEventConecction
   * @param obj El objeto ClusterNetConnectionListener
   * @return ClusterNetExcepcion Se lanza cuando ocurre un error al registrar el
   *  objeto callback
   */
  public void addConnectionListener(ClusterNetConnectionListener obj)
  {
     this.socketClusterNetImp.addPTMFConexionListener(obj);
  }


  //==========================================================================
  /**
   * Devuelve los id_socket de todos los emisores actuales.
   * @return un treeMap de ID_Sockets emisores
   */
  public TreeMap getTxMembers()
  {
   return this.socketClusterNetImp.getID_SocketEmisores ();
  }

  //==========================================================================
  /**
   * Elimina un objeto ClusterNetConnectionListener
   * @param un objeto ClusterNetConnectionListener
   */
  public void removeConnectionListener(ClusterNetConnectionListener obj)
  {
    this.socketClusterNetImp.removePTMFConexionListener(obj);
  }

  //==========================================================================
  /**
   * Un método para registrar un objeto que implementa la interfaz ClusterNetErrorListener.
   * La interfaz ClusterNetErrorListener se utiliza para notificar a las clases que se
   * registren de un evento ClusterNetEventError
   * @param obj El objeto ClusterNetErrorListener
   */
  public void addErrorListener(ClusterNetErrorListener obj)
  {
    this.socketClusterNetImp.addPTMFErrorListener(obj);
  }

  //==========================================================================
  /**
   * Elimina un objeto ClusterNetErrorListener
   * @param onj El objeto ClusterNetErrorListener
   */
  public void removeErrorListener(ClusterNetErrorListener obj)
  {
    this.socketClusterNetImp.removePTMFErrorListener(obj);
  }



  //==========================================================================
  /**
   * Un método para registrar un objeto que implementa la interfaz ClusterNetGroupListener.
   * La interfaz ClusterNetGroupListener se utiliza para notificar a las clases que se
   * registren de un evento ClusterNetEventGroup
   * @param obj El objeto ClusterNetGroupListener
   * @return ClusterNetExcepcion Se lanza cuando ocurre un error al registrar el
   *  objeto callback
   */
  public void addGroupListener(ClusterNetGroupListener obj)
  {
    this.socketClusterNetImp.addPTMFIDGLListener(obj);
  }

  //==========================================================================
  /**
   * Elimina un objeto ClusterNetGroupListener
   * @param obj El objeto ClusterNetGroupListener
   */
  public void removeGroupListener(ClusterNetGroupListener obj)
  {
    this.socketClusterNetImp.removePTMFIDGLListener(obj);
  }


  //=========================================================================
  /**
   * Un método para registrar un objeto que implementa la interfaz ClusterNetMemberListener.
   * La interfaz ClusterNetMemberListener se utiliza para notificar a las clases que se
   * registren de un evento ClusterNetEventGroup
   * @param obj El objeto ClusterNetMemberListener
   * @return ClusterNetExcepcion Se lanza cuando ocurre un error al registrar el
   *  objeto callback
   */
  public void addMemberListener(ClusterNetMemberListener obj)
  {
    this.socketClusterNetImp.addPTMFID_SocketListener(obj);
  }

  //==========================================================================
  /**
   * Un método para registrar un objeto que implementa la interfaz PTMFDatosRecibidosListener.
   * La interfaz PTMFDatosRecibidosListener se utiliza para notificar a las clases que se
   * registren de un evento PTMFEventIDGL
   * @param obj El objeto Indohandler.
   * @return PTMFExcepcion Se lanza cuando ocurre un error al registrar el
   *  objeto callback
   */
  public void addRxDataListener(ClusterNetRxDataListener obj)
  {
    this.socketClusterNetImp.addPTMFDatosRecibidosListener(obj);
  }

  //==========================================================================
  /**
   * Elimina un objeto PTMFDatosRecibidosListener
   */
  public void removeRxDataListener(ClusterNetRxDataListener obj)
  {
    this.socketClusterNetImp.removePTMFDatosRecibidosListener(obj);
  }
  //==========================================================================
  //==========================================================================
  /**
   * Elimina un objeto ClusterNetMemberListener
   * @param El objeto ClusterNetMemberListener
   */
  public void removeMemberListener(ClusterNetMemberListener obj)
  {
    this.socketClusterNetImp.removePTMFID_SocketListener(obj);
  }

  //==========================================================================
  /**
   * Devuelve el tamaño del buffer de emisión.
   * @return un int con el tamaño del buffer de emisión
   */
  public int getCapacityTxQueue()
  {
    return (socketClusterNetImp.getCapacidadColaEmision());
  }

  //==========================================================================
  /**
   * Establece el tamaño del buffer de emisión
   * @param iSize tamaño del buffer de emisión
   */
  public void setCapacityTxQueue(int isize)
  {
     socketClusterNetImp.setCapacidadColaEmision(isize);
  }

  //==========================================================================
  /**
   * Devuelve el tamaño del buffer de recepción
   * @return un int con el tamaño del buffer de recepción
   */
  public int getCapacityRxQueue()
  {
    return socketClusterNetImp.getCapacidadColaRecepcion();
  }

  //==========================================================================
  /**
   * Establece el tamaño del buffer de recepción
   * @param isize tamaño del buffer de recepción
   */
  public void setCapacityRxQueue(int isize)
  {
     socketClusterNetImp.setCapacidadColaRecepcion(isize);
  }

  //==========================================================================
  /**
   * Establece el tiempo de espera máximo que el thread de usuario espera
   * en una llamada al método receive() sin que hallan llegado datos.
   * @param iTiempo Tiempo máximo de espera en mseg. 0 espera infinita.
   */
  public void setSoTimeOut(int iTiempo)
  {
     socketClusterNetImp.setSoTimeOut(iTiempo);
  }

  //==========================================================================
  /**
   * Establece el tiempo de espera máximo que el thread de usuario espera
   * en una llamada al método receive() sin que hallan llegado datos.
   * @param iTiempo Tiempo máximo de espera en mseg. 0 espera infinita.
   */
  public int getSoTimeOut()
  {
     return socketClusterNetImp.getSoTimeOut();
  }

  //==========================================================================
  /**
   * Devuelve un TreeMap con los ID_Sockets del Grupo Local
   * @return TreeMap con los ID_Sockets del Grupo Local.
   */
   public TreeMap getMembers()
   {
      return this.socketClusterNetImp.getID_Sockets();
   }

  //==========================================================================
  /**
   * Devuelve un TreeMap con los IDGLs a los que alcanzamos
   * @return TreeMap con los IDGLs a los que alcanzamos
   */
   public TreeMap getGroups()
   {
      return this.socketClusterNetImp.getIDGLs();
   }


  //==========================================================================
  /**
   * Obtener el numero de IDGLs
   * @return int el numero de IDGLs
   */
   public int getNumGroups()
   {
        return this.socketClusterNetImp.getNumeroIDGLs();
   }

  //==========================================================================
  /**
   * Obtener el numero de IDSockets
   * @return int el numero de ID_Sockets
   */
   public  int getNumMembers()
   {
        return this.socketClusterNetImp.getNumeroID_Sockets();
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
  public long setRatioTx (long bytes_x_seg)
  {
    if(this.socketClusterNetImp!= null)
    return this.socketClusterNetImp.setRatioUsuario(bytes_x_seg);
    return 0;
  }


 /**
  * Establece el nivel de depuración.
  * @param i El nivel de depuracion. Debe de tomar uno de los sigioentes valores hexadecimales:
  *  NO DEPURAR      = 0;
  *  TODO            = 0xFFFFFFFF;
  *  ClusterGroupID            = 0x00000001;
  *  TPDU            = 0x00000002;
  *  TPDU_CGL        = 0x00000004;
  *  TPDU_DATOS      = 0x00000008;
  *  TEMPORIZADOR    = 0x00000010;
  *  CGL             = 0x00000020;
  *  SOCKET          = 0x00000040;
  *  CANAL_MULTICAST = 0x00000080;
  *  ACK             = 0x00000100;
  *  TPDU_NORMAL     = 0x00000200;
  *  NACK            = 0x00000400;
  *  HACK            = 0x00000800;
  *  HSACK           = 0x00001000;
  *  MACK            = 0x00002000;
  *  CGLOCALES       = 0x00004000;
  *  ID_SOCKETS_EMISORES   = 0x00008000;
  *  TABLA_ASENTIMIENTOS = 0x00020000;
  *  VENTANA         = 0x00080000;
  *  DATOS_THREAD    = 0x00100000;
  *  TPDU_RTX        = 0x00200000;
  *  DATOS_USUARIO   = 0x00800000;
  *  HNACK           = 0x01000000;
  *  ID_SOCKET       = 0x02000000;
  */
  public static void setLogLevel(int i)
  {
    if( i != 0)
    {
     Log.setDepuracion(true);
     Log.setNivelDepuracion(i);
    }
    else
    {
      Log.setDepuracion(false);
    }

  }
  
  

  //==========================================================================
  /**
   * Devuelve el número de bytes disponibles para leer.
   * @return Un entero con el número de bytes disponibles para ser leídos.
   */
  public int available()
  {
    return this.socketClusterNetImp.getColaRecepcion().getTamaño();
  }

  //==========================================================================
  /**
   * Envía los datos pasados por argumento al MulticastChannel.<br>
   * Si la cola de emisión está llena el método bloquea el hilo llamante
   * hasta que halla espacio en la cola de emisión.
   * LA COLA no bloqueará si se ha llamado a la función setSotimeOut() con
   * un valor mayor que cero.
   * @param datos Array de bytes a enviar
   * @exception IOException si se produce alguna excepción en el flujo de salida.
   */
  public boolean send(Buffer datos) throws IOException
  {
    return this.socketClusterNetImp.getColaEmision().add(datos.getBuffer(),0,datos.getBuffer().length,false);
  }

  //==========================================================================
  /**
   * Envía los datos pasados por argumento al MulticastChannel.<br>
   * Si la cola de emisión está llena el método bloquea el hilo llamante
   * hasta que halla espacio en la cola de emisión.
   * LA COLA no bloqueará si se ha llamado a la función setSotimeOut() con
   * un valor mayor que cero.<BR>
   * <STRONG>Con este método se puede especificar el Fin de una Transmisión
   * MULTICAST, LA CONEXIÓN SIGUE ACTIVA, ES UN MÉTODO DE AYUDA AL NIVEL
   * DE APLICACIÓN PARA DETECTAR FIN DE TRANSMISIÓN. </STRONG>
   * @param datos Array de bytes a enviar
   * @param bFinTransmision Especifica un final de una transmisión
   * @exception IOException si se produce alguna excepción en el flujo de salida.
   */
  public boolean send(Buffer datos,boolean bFinTransmsion) throws IOException
  {
    return this.socketClusterNetImp.getColaEmision().add(datos.getBuffer(),0,datos.getBuffer().length,bFinTransmsion);
  }

  //==========================================================================
  /**
   * Devuelve un objeto RegistroID_Socket_Buffer. El RegistroID_Socket_Buffer
   * contiene el identificador del socket que envió los datos y los propios datos.<br>
   * La llamada a este método es bloqueante, bloquea el hilo llamante hasta que
   * no halla ningún dato que leer.
   * @param id_socket El ClusterMemberID del socket que envía la información
   * @param datos Objeto Buffer con los datos recibidos del socket id_socket.
   * @return True si se han obtenidos datos, False en caso contrario
   * @exception IOException Se lanza si no se puede crear el flujo de salida.
   */
  public RegistroID_Socket_Buffer receive() throws IOException
  {
    return this.socketClusterNetImp.getColaRecepcion().remove();
  }

  
  //==========================================================================
  /**
   * Metodo main()
   */
  public  static void main(String[] string)
  {
   Log.log("","Iniciando Socket....");

   try
   {

     Log.setDepuracion(true);
     Log.setNivelDepuracion(Log.TODO);
     Address dirMcast = new Address ("224.100.100.100",2000);

     ClusterNet s = new ClusterNet(dirMcast,null,(byte)2,MODE_RELIABLE);

     while(true);
   }
   catch(UnknownHostException e)
   {
    Log.log("",""+e);
   }
   catch(ClusterNetExcepcion e)
   {
    Log.log("",""+e);
   }
   catch(ClusterNetInvalidParameterException e)
   {
    Log.log("",""+e);
   }
  }

  }

