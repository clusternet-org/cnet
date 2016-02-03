//============================================================================
//
//	Copyright (c) 1999-2015 . All Rights Reserved.
//
//----------------------------------------------------------------------------
//
//	File: CGLThread.java  1.0 13/10/99
//
//	Description: Class CGLThread.
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

import java.util.TreeMap;
import java.util.Hashtable;
import java.util.WeakHashMap;
import java.util.Random;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.Vector;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Thread que implementa la maquina de estados CGL.<br>
 * Thread de procesamiento de TPDUs CGL.<br>
 * Procesamiento interno de la clase ClusterNet.
 * @see ClusterNet
 * @author M. Alejandro Garcï¿½a Domï¿½nguez
 * <A HREF="mailto:alejandro.garcia.dominguez@gmail.com">(alejandro.garcia.dominguez@gmail.com)</A><p>
 *			   Antonio Berrocal Piris
 */

class CGLThread extends Thread
{
  //== CONSTANTES ===========================================================

  /** Nï¿½ de mensajes CGL redundantes mandados por la red.*/
  private static final short REDUNDANCIA_CGL = 2;

  /** Tiempo de redundancia. Tiempo a esperar entre las TPDU redundantes */
  private static final int TIEMPO_REDUNDANCIA = 10; //10 mseg.

  /** Nï¿½ Mï¿½ximo de intentos de bï¿½squeda de un grupo local.*/
  private static final short MAX_INTENTOS_BUSQUEDA_GRUPO_LOCAL = 2;

  /** Timepo minimo de espera en temporizadores */
  private static final int T_BASE = 10;

  /** Nï¿½ Mï¿½ximo de intentos de bï¿½squeda de CGL para un ClusterGroupID Emisor.*/
  private static final short MAX_INTENTOS_BUSQUEDA_GL_EMISOR = 1;

  /** TiME OUT para un TPDU SOCKET_ACEPTADO_EN_GRUPO_LOCAL*/
  private static final int T_ESPERA_ACEPTACION_EN_GRUPO_LOCAL = 2000; //2 Seg.

  /** Nï¿½ MAX. de intentos de espera aceptacion en Grupo Local */
  private static final short MAX_INTENTOS_ESPERA_ACEPTACION_GRUPO_LOCAL = 2;

  /**
   * Tiempo para TEMPORIZADOR TesperaGL.
   * Tiempo a esperar despuï¿½s de enviar una peticiï¿½n
   * de bï¿½squeda de grupo local  (TPDU_CGL_BUSCAR_GRUPO_LOCAL)
   * y antes de enviar otra
   */
  private static final int T_ESPERA_BUSQUEDA_GL = 2000;//5000; //msg

  /**
   * Tiempo para TEMPORIZADOR TesperaGLVECINO.
   * Tiempo a esperar despuï¿½s de enviar una peticiï¿½n
   * de bï¿½squeda de grupo local vecino (TPDU_CGL_BUSCAR_GRUPO_LOCAL_VECINO)
   * y antes de enviar otra.
   */
  private static final int T_ESPERA_BUSQUEDA_GL_VECINO = 2000;//5000; //msg

  /**
   * Tiempo para TEMPORIZADOR TesperaAceptacionGL.
   * Tiempo de espera de aceptaciï¿½n de un
   * mensaje ACEPTADO_EN_GRUPO_LOCAL.
   */
  private static final int T_ESPERA_ACEPTACION_GL = 2000;//4000; //mseg

  /**
   * Tiempo para TEMPORIZADOR TretrasoEsperaBuesquedaCGEmisor
   */
  private static final int T_ESPERA_BUSQUEDA_GL_EMISOR = 6000;//6000; // msg

  /**
   * Tiempo para TEMPORIZADOR TretrasoNotificacionGL.
   * Tiempo aleatorio de retraso antes
   * de enviar un mensaje GRUPO_LOCAL
   */
  private static final int T_RETRASO_NOTIFICACION_GL = 500; // msg

  /**
   * Tiempo para TEMPORIZADOR TretrasoNotificacionGLVecinos.
   * Tiempo aleatorio de retraso antes
   * de enviar un mensaje GRUPO_LOCAL "SOLO" cuando se ha recibido un
   *  TPDU BUSCAR_GRUPO_LOCAL_VECINO
   */
  private static final int T_RETRASO_NOTIFICACION_GL_VECINO = 1000; // msg


  /**
   * Tiempo para TEMPORIZADOR TRetrasoNotificacionGLSocketAceptado.
   * Tiempo aleatorio de retraso antes
   * de enviar un mensaje SOCKET_ACEPTADO_EN_GRUPO_LOCAL.
   */
  private static final int T_RETRASO_NOTIFICACION_SOCKET_ACEPTADO = 600; // msg


   /**
   * Tiempo para TEMPORIZADOR TretrasoNotificacionCGParaEmisor
   */
  private static int T_RETRASO_NOTIFICACION_GL_PARA_EMISOR = 600; //msg


  /** Tiempo de retraso para volver a procesar un estado de la mï¿½quina de estados
      CGL  */
  private static final int   TIEMPO_RETRASO_PROCESO_MAQUINA_ESTADO_CGL = 2; // msg

  /** Nï¿½mero Mï¿½ximo de sockets en el grupo local.  */
  private int N_MAX_SOCKETS_GL = ClusterNet.MAX_SOCKETS_GL;

  /**
   * Evento Notificar GL VECINOS.
   * Se manda un TPDU GRUPO_LOCAL cuando expira el timer T_RETRASONOTIFICACIONGLVECINO
   * Este temporizador se activa cuando se recibe el TPDU BUSCAR_GRUPO_LOCAL_VECINOS
   */
  protected static final int  EVENTO_NOTIFICAR_GL_VECINOS = 1;

  /**
   * Evento Notificar GL
   * Se manda un TPDU GRUPO_LOCAL cuando expira el timer T_RETRASO_NOTIFICACION_GL
   * Este temporizador se activa cuando se recibe el TPDU BUSCAR_GRUPO_LOCAL
   */
  protected static final int  EVENTO_NOTIFICAR_GL = 2;
  /** Evento Notificar CG PARA EMISOR*/
  protected static final int  EVENTO_NOTIFICAR_GL_EMISOR = 7;
  /** Evento Notificar Aceptaciï¿½n de socket*/
  protected static final int  EVENTO_ACEPTAR_SOCKET = 3;
  /** Evento BUSCAR GL*/
  protected static final int  EVENTO_BUSCAR_GL = 4;
  /** Evento BUSCAR GL Vecino*/
  protected static final int  EVENTO_BUSCAR_GL_VECINO = 5;
  /** Evento BUSQUEDA DE CG PARA EMISOR*/
  protected static final int  EVENTO_BUSCAR_GL_EMISOR = 6;
  /** Evento TIME OUT espera de TPDU SOCKET_ACEPTADO_EN_GRUPO_LOCAL*/
  protected static final int  EVENTO_TIME_OUT = 8;



  //== PROTOCOLO CGL ========================================================
  /** ClusterGroupID al que pertenece este socket. */
  private ClusterGroupID clusterGroupID = null;

  /** TreeMap de sockets que pertenecen al grupo local. KEY= IPv4 VALUE=null */
  private TreeMap treeMapID_Socket = null;


  /**
   * TreeMap de IDGLs.
   * KEY= ClusterGroupID
   * VALUE= TreeMap de IDGLS a los que llega el "ClusterGroupID" KEY.
   */
  private TreeMap treeMapIDGLs = null;


  /**
   * WeakHashMap Cache
   * KEY= IDGLFuente
   * VALUE= IDGLs Padres
   */
  private WeakHashMap cachePadres = null;
  /** La capacidad de la cache Padre */
  private int CAPACIDAD_CACHE_PADRES = 50;

  /**
   * WeakHashMap Cache Hijos
   * KEY= IDGLFuente
   * VALUE= IDGLs Hijos
   */
  private WeakHashMap cacheHijos = null;
  /** La capacidad de la cache Hijo */
  private int CAPACIDAD_CACHE_HIJOS = 50;


  /** Nï¿½mero de sockets actuales en el grupo local*/
  private int N_SOCKETS = 0;

  /**
   * TTL usado en los mensajes CGL. Distinto del de la sesiï¿½n, utilizado en el
   *   anillo de expansiï¿½n en la bï¿½squeda de grupos locales.
   */
  private byte TTL = 0;


  //== VAR. DE LA CLASE ======================================================
  /** Un flag utilizado para iniciar/parar este thread. */
  private boolean  runFlag = true;

  /** El socket ClusterNet */
  private SocketClusterNetImp socketClusterNetImp = null;

  /** Estado de la mï¿½quina de estados CGL */
  private int estadoCGL = ClusterNet.ESTADO_CGL_NULO;

  /**
   * Semï¿½foro para parar a DataThread cuando nos pregunta por IDGLsPadres y
   * tenhemos que mANDAR un tpdu buscar_cg_emisor
   */
  private Semaforo semaforoDatosThread = null;

  /**  Nï¿½ de Intentos (Bï¿½squeda de grupo, ï¿½tc.) */
  private short N_INTENTOS = 0;

  /** TPDU CGL */
  private TPDUCGL  tpduCGL   = null;

  /** Direccion del emisor del TPDU CGL */
  private Address  src       = null;

  /** Boolean. Buscar GL */
  private boolean bBuscarGL  = false;

  /** Boolean. Buscar GL Vecino*/
  private boolean bBuscarGLVecino  = false;

  /** Boolean para Aceptaciï¿½n de un socket en el grupo local */
  private boolean bAceptarSocket = false;

  /** Boolean para Notificaciï¿½n de Grupo Local*/
  private boolean bNotificarGL = false;

  /** Boolean para Notificaciï¿½n de Grupo Local a los grupos vecinos */
  private boolean bNotificarGLVecinos = false;

  /** Boolean para Notificaciï¿½n de BUSQUEDA DE EMISOR */
  private boolean bNotificarBusquedaEmisor = false;

  /** Boolean para Notificaciï¿½n de CG Emisor*/
  private boolean bNotificarCGEmisor = false;

  /** Boolean para indicar que se ha lanzado un ClusterTimer para enviar un TPDU GL */
  private boolean bLanzadoTemporizadorNotificacionGL = false;

  /** Boolean para indicar que se ha lanzado un ClusterTimer para enviar un TPDU GL VECINO*/
  private boolean bLanzadoTemporizadorNotificacionGLVecino = false;

  /** Boolean para Retransmitir UNIRSE_A_GRUPO_LOCAL */
  private boolean bRetransmitirUnirseGrupoLocal = false;
  /** Objeto Random */
  private Random random = null;

  /** ClusterTimer  */
  private TimerHandler timerHandler = null;

  /** Cola de aceptaciï¿½n de sockets */
  private  LinkedList colaAceptacionID_SOCKET = null;

  /** Lista de interface de notificaciï¿½n ClusterMemberID */
  private LinkedList listaId_SocketListener = null;

  /** Lista de interfaces de notificaciï¿½n ID_SOCKET */
  private  LinkedList listaIDGLListener = null;

  /** Lista de Bï¿½squeda de CG para Emisores...*/
  //
  //  key = idgl_emisor
  //  value = clase Intentos_Emisor
  //
  private  TreeMap treeMapBusquedaEmisores = null;

  /** Lista de repuesta a Busqueda de CG para Emisores...*/
  private  TreeMap treeMapRespuestaBusquedaEmisores = null;

  /** Lista de Grupos que buscan GRUPOS_LOCALES_VECINOS...*/
  private  TreeMap treeMapRespuestaGLVecinos = null;

  /** TTL de notificaciï¿½n del Grupo Local */
  private short TTL_Notificacion_GL = 1;

  private short TTL_BUSCAR_GRUPO_LOCAL_VECINO = 1;

  //==========================================================================
  /**
   * Constructor. Establece el thread como un thread demonio.
   */
  CGLThread (SocketClusterNetImp socketClusterNetImp)
  {
    super("CGLThread");

    //PRIORIDAD..
    //this.setPriority(Thread.MAX_PRIORITY - 2);

    if(socketClusterNetImp != null)
    {
      //
      // Thread demonio...
      //
      setDaemon(true);
      this.socketClusterNetImp = socketClusterNetImp;
      this.runFlag = true;

      try
      {
        //
        // Crear hash,treemap,linkedlist,
        //   obtener direcciï¿½n unicast local, ...
        //
        this.treeMapIDGLs = new TreeMap();
         this.colaAceptacionID_SOCKET = new LinkedList();
        this.treeMapBusquedaEmisores = new TreeMap();
        this.treeMapRespuestaBusquedaEmisores = new TreeMap();
        this.treeMapRespuestaGLVecinos = new TreeMap();
        this.listaIDGLListener = new LinkedList();
        this.listaId_SocketListener = new LinkedList();
        this.random = new Random();
        this.treeMapID_Socket = new TreeMap();

        //Semï¿½foro...
        this.semaforoDatosThread = new Semaforo(true,1);

        //
        //CACHES...
        cachePadres = new WeakHashMap(CAPACIDAD_CACHE_PADRES,1);
        cacheHijos = new WeakHashMap(CAPACIDAD_CACHE_HIJOS,1);

        //
        // TPDUCGL y Direccion del emisor
        //
        this.src       = new Address();

        //
        // Iniciar Temporizadores...
        //
        this.timerHandler  =
         new TimerHandler()
         {
             public void TimerCallback(long arg1, Object o)
             {
               switch((int)arg1)
               {
                case EVENTO_NOTIFICAR_GL:
                  Log.debug(Log.CGL,"CGLThread.timerHandler","EVENTO_NOTIFICAR_GL");
                  bNotificarGL = true;
                  break;
                case EVENTO_NOTIFICAR_GL_VECINOS:
                  Log.debug(Log.CGL,"CGLThread.timerHandler","EVENTO_NOTIFICAR_GL_VECINOS");
                  bNotificarGLVecinos = true;
                  break;
                case EVENTO_ACEPTAR_SOCKET:
                  Log.debug(Log.CGL,"CGLThread.timerHandler","EVENTO_ACEPTAR_SOCKET");
                  bAceptarSocket = true;
                  break;
                case EVENTO_BUSCAR_GL:
                  Log.debug(Log.CGL,"CGLThread.timerHandler","EVENTO_BUSCAR_GL");
                  bBuscarGL = true;
                  break;
                case EVENTO_BUSCAR_GL_VECINO:
                  Log.debug(Log.CGL,"CGLThread.timerHandler","EVENTO_BUSCAR_GL_VECINO");
                  bBuscarGLVecino = true;
                  break;
                case EVENTO_BUSCAR_GL_EMISOR:
                  Log.debug(Log.CGL,"CGLThread.timerHandler","EVENTO_BUSCAR_EMISOR");
                  bNotificarBusquedaEmisor = true;
                  break;
                case EVENTO_NOTIFICAR_GL_EMISOR:
                  Log.debug(Log.CGL,"CGLThread.timerHandler","EVENTO_GL_EMISOR");
                  bNotificarCGEmisor = true;
                  break;
                case EVENTO_TIME_OUT:
                  //Este evento se utiliza en las esperas del TPDU SOCKET_ACEPTADO_EN_GRUPO_LOCAL
                  Log.debug(Log.CGL,"CGLThread.timerHandler","EVENTO_TIME_OUT");
                  //setEstadoCGL(ClusterNet.ESTADO_CGL_BUSCAR_GL);
                  N_INTENTOS++;
                  if(N_INTENTOS > MAX_INTENTOS_ESPERA_ACEPTACION_GRUPO_LOCAL)
                  {
                    setEstadoCGL(ClusterNet.ESTADO_CGL_BUSCAR_GL);
                    N_INTENTOS = 0;
                  }
                  else
                    bRetransmitirUnirseGrupoLocal = true;

                  break;
               }
             }
          };

      }
      catch(UnknownHostException e){error(e);}
      catch(ClusterNetInvalidParameterException e){error(e);}

    }
  }


  //==========================================================================
  /**
   * Aï¿½adir una interface ClusterMemberListener.
   * @param IDSocketListener
   */
   void addID_SocketListener(ClusterMemberListener id_socketListener)
   {
      listaId_SocketListener.add(id_socketListener);
   }

  //==========================================================================
  /**
   * Eliminar una interface ClusterMemberListener.
   * @param IDSocketListener
   */
   void removeID_SocketListener(ClusterMemberListener id_socketListener)
   {
     listaId_SocketListener.remove(id_socketListener);
   }

  //==========================================================================
  /**
   * Aï¿½ade una interface ClusterGroupListener.
   * @param ClusterGroupListener
   */
   void addIDGLListener(ClusterGroupListener idglListener)
   {
      this.listaIDGLListener.add(idglListener);
   }

  //==========================================================================
  /**
   * Elimina una interface ClusterGroupListener.
   * @param ClusterGroupListener
   */
   void removeIDGLListener(ClusterGroupListener idglListener)
   {
      this.listaIDGLListener.remove(idglListener);
   }

  //==========================================================================
  /**
   * El mï¿½todo run del thread. <br>
   * Procesa TPDU recibidos CGL y datos.<br>
   * Gestiona la mï¿½quina de estados CGL.<br>
   * Gestiona la fiabilidad de los datos y el control de flujo y congestiï¿½n.
   */
  public void run()
  {
    final String    mn = "SocketPTMFThread.run()";
    Buffer          buf;
    Address         src;


    //
    // Bucle infinito.
    //
    Log.debug(Log.CGL,"CGLThread","INICIADO");
    while (this.runFlag || (getEstadoCGL()!= ClusterNet.ESTADO_CGL_NULO ))
    {

      //
      // Procesar estado y TPDUs CGL
      //
      maquinaEstadoCGL();

      if (!this.runFlag)
       {
          Log.debug(Log.CGL,mn,"Fin del hilo CGL.");
          return;
       }

      //
      // Esperar un tiempo para volver a procesar...
      //
      this.socketClusterNetImp.getTemporizador().sleep(TIEMPO_RETRASO_PROCESO_MAQUINA_ESTADO_CGL);
      //this.socketPTMFImp.getTemporizador().yield();
    }
    Log.debug(Log.CGL,"CGLThread","FINALIZADO");

  }


  //==========================================================================
  /**
   * El mï¿½todo stop del thread. Pone a false el flag runFlag provocando que
   * el thread finalice. No llama al mï¿½todo stop() de la clase Thread.
   */
  void stopThread()
  {
    //
    // CAMBIAR DE ESTADO --> DEJAR_GL
    //
    this.setEstadoCGL(ClusterNet.ESTADO_CGL_DEJAR_GL);
    runFlag = false;
  }

  //==========================================================================
  /**
   * Este mï¿½todo implementa la mï¿½quina de estados CGL.<br>
   * El estado de la mï¿½quina se almacena en la variable estadoCGL.
   */
  private void maquinaEstadoCGL()
  {
    int      estadoCGL = 0;

    //
    // Obtener el estado CGL...
    //
    estadoCGL = getEstadoCGL();

    switch(estadoCGL)
    {
      case ClusterNet.ESTADO_CGL_NULO:
        maquinaEstadoCGL_NULO();
        break;
      case ClusterNet.ESTADO_CGL_BUSCAR_GL:
        maquinaEstadoCGL_BUSCAR_GL();
        break;
      case ClusterNet.ESTADO_CGL_ESPERAR_ACEPTACION_GL:
        maquinaEstadoCGL_ESPERAR_ACEPTACION_GL();
        break;
      case ClusterNet.ESTADO_CGL_MIEMBRO_GL:
        maquinaEstadoCGL_MIEMBRO_GL();
        break;
      case ClusterNet.ESTADO_CGL_CREAR_GL:
        maquinaEstadoCGL_CREAR_GL();
        break;
      case ClusterNet.ESTADO_CGL_BUSCAR_GL_VECINOS:
        maquinaEstadoCGL_BUSCAR_GL_VECINOS();
        break;
      case ClusterNet.ESTADO_CGL_DEJAR_GL:
        maquinaEstadoCGL_DEJAR_GL();
        break;
      case ClusterNet.ESTADO_CGL_MONITOR:
        maquinaEstadoCGL_MONITOR();
        break;
      default:
        Log.log("CGLThread.maquinaEstadoCGL()","estado CGL incorrecto.");
    }
  }


  //==========================================================================
  /**
   * Se devuelve un TreeMap con los IDGLs que actï¿½an como CG "Padres" para
   * este socket dado el ClusterGroupID del emisor.
   *
   * POLï¿½TICA:
   *
   * Un ClusterGroupID Padre es todo aquel ClusterGroupID que hemos recibido mediante un mensaje
   *  GRUPO_LOCAL_VECINO

   *  DEVOLVER:
   *
   * 1ï¿½ Caso: Si el socket emisor es este o pertenece a este GL.
        -	No existen GL Padres.
   *
   * 2ï¿½ Caso: Si el socket emisor NO Pertenece a este GL.
        1.Si el ClusterGroupID del emisor estï¿½ en la lista listaIDGLs el ClusterGroupID emisor es
          alcanzable por este GL y por lo tanto serï¿½ GL Padre.
        2.Si el ClusterGroupID del emisor no estï¿½ en la lista listaIDGLs se recorre
          la lista listaIDGLs para averiguar si en las sublistas de cada ClusterGroupID
          estï¿½ el ClusterGroupID Emisor, aquellos IDGLs en cuyas sublistas se halle
          el ClusterGroupID del emisor serï¿½n GLs Padres.
        3.Si no se encuentra el ClusterGroupID Emisor en la lista ni en las sublistas
          de los IDGLs, se recurre a una bï¿½squeda especï¿½fica
          tal como se describe en la secciï¿½n 6.15



   *  -Si llegamos directamente al emisor devolvemos el Emisor
   *  -Si no llegamos ver si algï¿½nh ClusterGroupID Padre Potencial llega y devolver
   *    la lista de aquellos que lleguen.
   *  -Si no hay ningï¿½n ClusterGroupID que llegue al ClusterGroupID Emisor buscarlo enviando un
   *    mensaje BUSCAR_GL_PARA_EMISOR. SE BLOQUEA EL THREAD LLAMANTE HASTA QUE
   *    SE OBTENGA UNA RESPUESTA O SE LLEGUE A UN TIME-OUT..
   *
   * @param idglEmisor ClusterGroupID del emisor para el que tenemos que buscar CG "Padres".
   *  Los ClusterGroupID estï¿½n en Key, Value es siempre NULL.
   * @return Devuelve un objeto TreeMap con los IDGLs de los Controladores de
   *  grupo padres para este emisor.
   */
  TreeMap getCGPadres(ClusterGroupID idglEmisor)
  {
      final String mn = "CGLThread.getCGPadres (clusterGroupID)";
      TreeMap treemap = null;


      if (idglEmisor==null)
        return new TreeMap();

      //1ï¿½ Caso: El ClusterGroupID emisor es igual a este.
      if (idglEmisor.equals (this.getIDGL()))
      {
        //DEPURACION
        Log.debug(Log.CGL,mn,"No hay IDGLS Padres!");//Log.log("getCGhIJOS() == 0","");

        return new TreeMap();
      }

      //
      // 1ï¿½. Comprobar si estï¿½ en la cache....
      // NOTA: Si llegan nuevos datos que afecten a la cache, esta elimina la
      // entrada para el ClusterGroupID afectado
      //

      if( this.cachePadres.containsKey(clusterGroupID))
        return (TreeMap)this.cachePadres.get(clusterGroupID);

      //
      // 2.1
      //
      if( this.treeMapIDGLs.containsKey(idglEmisor) )
      {
        treemap = new TreeMap();

        Log.debug(Log.CGL,mn,"El ClusterGroupID emisor es directamente alcanzable");

        //Obtener el ClusterGroupID emisor del treemap , para obtener el TTL correcto...
        RegistroIDGL_TreeMap reg  = (RegistroIDGL_TreeMap) treeMapIDGLs.get(idglEmisor);
        treemap.put(reg.clusterGroupID,null);

        return treemap;
      }

      //
      //  2.2
      //
      // No llegamos directamente, ver si algï¿½n GL llega al GL solicitado
      //

      Iterator iterator = this.treeMapIDGLs.values().iterator();
      while(iterator.hasNext())
      {
          RegistroIDGL_TreeMap reg = (RegistroIDGL_TreeMap) iterator.next();

          if(reg.clusterGroupID.equals(this.clusterGroupID))
          {
            continue;
          }

          if((reg.treemap!= null) &&(reg.treemap.containsKey(idglEmisor)))
          {
            if(treemap == null)
            {
               treemap = new TreeMap();
            }

            //Aï¿½adir el ClusterGroupID que llega al "Emisor"....
            treemap.put(reg.clusterGroupID,null);

            Log.debug(Log.CGL,mn,"El ClusterGroupID emisor se alcanza por:"+ reg.clusterGroupID);


          }

       }



      if (treemap == null)
      {
        //
        //  2.3
        //
        //  No llegamos ni existe conocimiento de que algï¿½n Vecino llegue,
        //  "PO" PREGUNTAMOS....
        //
        //.... ï¿½bloqueamos cuando DataThread nos pregunte?
        //.... ï¿½Establecemos un callback? .....

        Log.debug(Log.CGL,mn,"El ClusterGroupID emisor no es directamente alcanzable por nadie, iniciando procedimiento de bï¿½squeda...");

        this.treeMapBusquedaEmisores.put(idglEmisor,new Intentos_Emisor(idglEmisor,(short)0));
        this.TTL_BUSCAR_GRUPO_LOCAL_VECINO = 1;
        enviarTPDU_CGL_BUSCAR_GL_PARA_EMISOR(idglEmisor);

        //
        //ClusterTimer de espera para volver a reintentar...
        //
        this.socketClusterNetImp.getTemporizador().registrarFuncion(this.T_ESPERA_BUSQUEDA_GL_EMISOR,
          timerHandler,this.EVENTO_BUSCAR_GL_EMISOR);

        //Bloquer a DataThread....
        //Log.log (mn,"HA DORMIDO AL HILO DE DATOS.");
        this.semaforoDatosThread.down();

        //Cuando nos despertemos, puede haber sucedido dos cosas:
        //1. Se ha encontrado un ClusterGroupID "Padre" para el ClusterGroupID preguntado y se ha
        // almacenado en la cache.... ï¿½
        //2. No se ha encontrado ningï¿½n ClusterGroupID "Padre" y no estï¿½ en la cache...
        if( this.cachePadres.containsKey(clusterGroupID))
        {
         return (TreeMap)this.cachePadres.get(clusterGroupID);
        }

      }
      else
      {   //Aï¿½adir a la cache...
         this.cachePadres.put(idglEmisor,treemap);
      }


     return treemap;
  }

  //==========================================================================
  /**
   * Obtener ClusterGroupID "Hijos" que dependen de este ClusterGroupID para el Control de la
   * Fiabilidad. Devuelve un TreeMap con los IDGLs
   *  que actï¿½an como "hijos" para este ClusterGroupID dado el ClusterGroupID de un determinado emisor.
   * @param idglEmisor ClusterGroupID del emisor para el que tenemos que buscar CG "Hijos".
   * @return Devuelve un objeto TreeMap con los IDGLs de los Controladores de
   *  de grupo hijos para este emisor.
   */
  private TreeMap getCGHijos()
  {
   return this.getCGHijos(this.clusterGroupID);
  }


  //==========================================================================
  /**
   * Devuelve un TreeMap con los IDGLs  que actï¿½an como "hijos" para
   * este socket dado un ClusterGroupID "Emisor".<br>
                                                    <br>
   *
   * POLï¿½TICA:<br>
   *          <br>


   *  1ï¿½ Caso: Si el socket emisor es este o pertenece a este GL
   *           - Todos los IDGLs de la lista listaIDGLs son considerados
   *             como GL Hijos

   *
   *  2ï¿½ Caso: Si el socket emisor NO es este o NO pertenece a este GL:
   *           -  Todos aquellos IDGLs de la lista listaIDGLs
   *                 (menos el ClusterGroupID emisor si estï¿½) son considerados como
   *                  GL Hijos solo si en la sublista de estos IDGLs
   *                  no aparece el ClusterGroupID emisor.


   * @param idglEmisor ClusterGroupID del emisor para el que tenemos que buscar CG "Hijos".
   * @return Devuelve un objeto TreeMap con los IDGLs de los Controladores de
   *  de grupo hijos para este emisor.
   */
  TreeMap getCGHijos(ClusterGroupID idglEmisor)
  {
      TreeMap treemap = null;
      String mn = "CGLThread.getCGHijos";

      if (idglEmisor==null || this.clusterGroupID==null)
        return new TreeMap();


      //
      // 1ï¿½. Comprobar si estï¿½ en la cache....
      // NOTA: Si llegan nuevos datos que afecten a la cache, esta elimina la
      // entrada para el ClusterGroupID afectado
      //
      if( this.cacheHijos.containsKey(clusterGroupID))
        return (TreeMap)this.cacheHijos.get(clusterGroupID);

      //
      // -1* 1ï¿½ CASO: Si el ClusterGroupID Emisor es el mismo que el nuestro:
      //          -->>>> devolver todos los IDGLs
      //
      if(idglEmisor.equals(this.clusterGroupID))
      {
       Log.debug(Log.CGL,mn,"Soy ClusterGroupID emisor");
       TreeMap treemap1 = (TreeMap)this.treeMapIDGLs.clone();
       treemap1.remove(this.clusterGroupID);
       //Aï¿½adir a la cache...
       this.cacheHijos.put(idglEmisor,treemap1);


       // DEPURACION:
       Log.debug(Log.CGL,mn,"IDGLS Hijos: ");
       if(treemap1 != null && treemap1.size() > 0)
       {
        Iterator iterator = treemap1.keySet().iterator();

         while(iterator.hasNext())
         {
           ClusterGroupID clusterGroupID = (ClusterGroupID) iterator.next();
           Log.debug(Log.CGL,"","ClusterGroupID --> "+clusterGroupID);
         }
       }

       return treemap1;
      }


      // -2* 2ï¿½ CASO: Si el ClusterGroupID Emisor Fuente no es este:
      //      -->>>> Devolver como ClusterGroupID Hijos:
      //            Aquellos IDGLs que no lleguen
      //            directamente al ClusterGroupID Emisor.
      //
      treemap = new TreeMap();

      if(this.treeMapIDGLs.keySet().size() > 0)
      {

        //Log.log("getCGHijos() 2.2","");
        //Recorrer todos los IDGLs, si llegan al "Emisor"
        //no incluirlos en la lista....

        Iterator iterator = this.treeMapIDGLs.values().iterator();

        while(iterator.hasNext())
        {
          RegistroIDGL_TreeMap reg = (RegistroIDGL_TreeMap)iterator.next();

          if(reg == null)
          {
          //  Log.log("reg NULL","");
            continue;
          }

          if(reg.treemap == null)
          { // Log.log("reg.treemap NULL","");
            continue;
          }

          if(reg.clusterGroupID == null)
          { // Log.log("reg.idgl NULL","");
                       continue;
          }

          //Si no es el "Emisor" ver si llega al "Emisor"....
          if( !reg.clusterGroupID.equals(idglEmisor) && !reg.clusterGroupID.equals(this.clusterGroupID) &&reg.treemap!= null && !reg.treemap.containsKey(idglEmisor) )
          {
                //El "ClusterGroupID" no llega al "Emisor",depende de nosostros...
                treemap.put(reg.clusterGroupID,null);
          }
        }


      //DEPURACION
      if (treemap.size() <= 0)
      {
        Log.debug(Log.CGL,mn,"IDGLS Hijos: 0");//Log.log("getCGhIJOS() == 0","");
      }
      else
      {
         Log.debug(Log.CGL,mn,"IDGLS Hijos: 0");
        //Log.log("getCGhIJOS().size() == ",""+treemap.size());
        Iterator iterator1 = treemap.keySet().iterator();

        while(iterator1.hasNext())
        {
          ClusterGroupID clusterGroupID = (ClusterGroupID) iterator1.next();
           Log.debug(Log.CGL,"","ClusterGroupID --> "+clusterGroupID);
          //Log.log("HIJO --> ClusterGroupID: "+clusterGroupID,"");

        }
      }



        //Aï¿½adir a la cache...
        this.cacheHijos.put(idglEmisor,treemap);
      }

      //Si no se encuentra nada---> treemap vacï¿½o.
      return treemap;
  }



 //==========================================================================
  /**
   * Obtener todos los ID_Sockets
   * @return treemap con ID_Sockets en KEY. Valor = null.
   */
   TreeMap getID_Sockets()
   {
     return (TreeMap)this.treeMapID_Socket.clone();
   }

  //==========================================================================
  /**
   * Obtener todos los IDGLs Vecinos
   * @return treemap con IDGLs en KEY. Valor = null.
   */
   TreeMap getIDGLs()
   {
     return (TreeMap)this.treeMapIDGLs.clone();
   }

  //==========================================================================
  /**
   * Obtener el numero de IDGLs
   * @return int
   */
   int getNumeroIDGLs()
   {
     return this.treeMapIDGLs.size();
   }

  //==========================================================================
  /**
   * Obtener el numero de IDSockets
   * @return int
   */
   int getNumeroID_Sockets()
   {
     return this.treeMapID_Socket.size();
   }

  //==========================================================================
  /**
   * Notificar nuevo ClusterMemberID.
   * @param ClusterMemberID Objeto ClusterMemberID nuevo.
   */
   private void notificarNuevoID_Socket(ClusterMemberID id_socket)
   {
     if (id_socket == null)
      return;

      ListIterator iterator = this.listaId_SocketListener.listIterator();
      while(iterator.hasNext())
      {
        ClusterMemberListener  id_socketListener = (ClusterMemberListener) iterator.next();
        id_socketListener.ID_SocketAñadido(id_socket);
      }

      //Y a los usuarios...
      this.socketClusterNetImp.sendPTMFEventAddID_Socket(""+id_socket,id_socket);


   }

  //==========================================================================
  /**
   * Notificar Eliminaciï¿½n ClusterMemberID.
   * @param ClusterMemberID Objeto ClusterMemberID eliminado.
   */
   private void notificarEliminacionID_Socket(ClusterMemberID id_socket)
   {
     if (id_socket == null)
      return;

      ListIterator  iterator = this.listaId_SocketListener.listIterator();
      while(iterator.hasNext())
      {
        ClusterMemberListener  id_socketListener = (ClusterMemberListener) iterator.next();
        id_socketListener.ID_SocketEliminado(id_socket);
      }
      //Y a los usuarios...
      this.socketClusterNetImp.sendPTMFEventRemoveID_Socket(""+id_socket,id_socket);


   }

  //==========================================================================
  /**
   * Notificar nuevo ClusterGroupID.
   * @param ClusterGroupID Objeto ClusterGroupID nuevo.
   */
   private void notificarNuevoIDGL(ClusterGroupID clusterGroupID)
   {
     if (clusterGroupID == null)
      return;

      ListIterator iterator = this.listaIDGLListener.listIterator();
      while(iterator.hasNext())
      {
        ClusterGroupListener  idglListener = (ClusterGroupListener) iterator.next();
        idglListener.IDGLAñadido(clusterGroupID);
      }
      //Y a los usuarios...
      this.socketClusterNetImp.sendPTMFEventAddIDGL(""+clusterGroupID,clusterGroupID);

   }

  //==========================================================================
  /**
   * Notificar Eliminaciï¿½n ClusterGroupID.
   * @param ClusterGroupID Objeto ClusterGroupID eliminado.
   */
   private void notificarEliminacionIDGL(ClusterGroupID clusterGroupID)
   {
     String mn = "CGLThread.notificarEliminacionIDGL";

     if (clusterGroupID == null)
      return;


      ListIterator iterator = this.listaIDGLListener.listIterator();
      while(iterator.hasNext())
      {
        ClusterGroupListener  idglListener = (ClusterGroupListener) iterator.next();
        idglListener.IDGLEliminado(clusterGroupID);
        Log.debug(Log.CGL,mn,"Notificado a DATOS_THREAD ELIMINACION ClusterGroupID: "+clusterGroupID);
      }
      //Y a los usuarios...
      this.socketClusterNetImp.sendPTMFEventRemoveIDGL(""+clusterGroupID,clusterGroupID);


   }

  //==========================================================================
  /**
   * Devuelve el TTL de los sockets del grupo local.
   * Esta funciï¿½n es necesario cuando los sockets de un grupo local
   * no estï¿½n en la misma subred.
   * @return
   */
   short getTTLSocketsGL()
   {
      //En esta implementaciï¿½n es SIEMPRE 1
      return 1;
   }


  //==========================================================================
  /**
   * Devuelve el TTL mï¿½s grande de todos los IDGLs contenidos en el treemap
   *  TreeMapIDGLVecinos. Devuelve como mï¿½nimo ttl=2.
   * @return El TTL Mï¿½ximo de todos los IDGLs contenidos en el treemap. Mï¿½nimo ttl = 2.
   */
   private short getTTLGLMaximo()
   {
      Iterator iterator = this.treeMapIDGLs.keySet().iterator();
      ClusterGroupID clusterGroupID = null;
      short ttl = 2;

      while(iterator.hasNext())
      {
        clusterGroupID = (ClusterGroupID) iterator.next();
        if (ttl < clusterGroupID.TTL)
          ttl = clusterGroupID.TTL;
      }
      //Log.log ("getTTLGLMaximo:",""+ttl);
      return ttl;
   }

  //==========================================================================
  /**
   * Devuelve el TTL mï¿½s grande de todos los IDGLs pasados mediante un TreeMap.
   * @param treemap TreeMap con IDGLS.
   * @return El TTL Mï¿½ximo de los IDGLs padres para un ClusterGroupID dado. Mï¿½nimo devuelve
   * el valor 1.
   */
   short getTTLGLMaximo(TreeMap treemap)
   {
      Iterator iterator = treemap.keySet().iterator();
      ClusterGroupID clusterGroupID = null;
      short ttl = 1;

      while(iterator.hasNext())
      {
        clusterGroupID = (ClusterGroupID) iterator.next();
        //Log.log("getTTLGLMaximo()--> ClusterGroupID: "+clusterGroupID,"");

        if (clusterGroupID.TTL > ttl)
          ttl = clusterGroupID.TTL;
      }

      //Log.log ("getTTLGLMaximo (treeMap):",""+ttl);
      return ttl;
   }


  //==========================================================================
  /**
   * Elimina un ID_SOCKET del treemap ID_SOCKETS. EL ID_SOCKET identifica a un
   *  socket que se ha caï¿½do o no responde y se quita de la lista.
   * @param id_socket El socket que se elimina de la lista.
   */
  void removeID_SOCKET(ClusterMemberID id_socket)
  {
    //Eliminar
    this.treeMapID_Socket.remove(id_socket);
    //Quitar nï¿½mero de sockets
    this.N_SOCKETS--;

    if(this.getEstadoCGL() == ClusterNet.ESTADO_CGL_MIEMBRO_GL)
    {
     //
     // enviar un TPDU notificando que el socket se elimina...
     //
     // NO--> Evitar posibles prï¿½cticas hackers.... :)
     //this.enviarTPDU_CGL_DEJAR_GRUPO_LOCAL(id_socket);
    }

    //Notificar eliminaciï¿½n a las clases de ClusterNet....
    this.notificarEliminacionID_Socket(id_socket);



  }

  //==========================================================================
  /**
   * Elimina un ClusterGroupID, del treemap treemapIDGLVecinos
   */
  void removeIDGL (ClusterGroupID clusterGroupID)
  {
    if (this.clusterGroupID.equals(clusterGroupID))
      return;

    //Eliminar el ClusterGroupID del Treemap
    this.treeMapIDGLs.remove(clusterGroupID);


    //Y de los Treemaps internos de Padres Potenciales...
    Iterator iterator = this.treeMapIDGLs.values().iterator();
    while(iterator.hasNext())
    {
       RegistroIDGL_TreeMap reg = (RegistroIDGL_TreeMap)iterator.next();
       if(reg.treemap!=null)
         reg.treemap.remove(clusterGroupID);
    }

    //Comprobar cache...
    comprobarCache(clusterGroupID);

    //Notificar eliminaciï¿½n a las clases de ClusterNet....
    this.notificarEliminacionIDGL(clusterGroupID);
   }

  //==========================================================================
  /**
   * Obtener un TreeMap de ID_Sockets de los MIEMBROS DE ESTE GRUPO.
   * @return Devuelve un objeto TreeMap de ID_SOCKETS.
   */
  private TreeMap getTreeMapID_Socket()
  {
   return this.treeMapID_Socket;
  }


 //==========================================================================
  /**
   * Obtener un TreeMap de ID_Sockets de los MIEMBROS VECINOS DE ESTE GRUPO.
   * Es decir, de los miembros de este grupo menos este socket.
   * @return Devuelve un objeto TreeMap de ID_SOCKETS.
   * Devuelve una copia: Las modificaciones no afectarï¿½n.
   */
  TreeMap getTreeMapID_SocketVecinos ()
  {
    //TreeMap result = (TreeMap)this.getTreeMapID_Socket().clone();
    TreeMap result = new TreeMap();

    result.putAll(this.treeMapID_Socket);

    result.remove (this.socketClusterNetImp.getID_Socket());


    //Log.log ("","Socket Vecinos: " + result);
    return result;
  }

  //==========================================================================
  /**
   * Devuelve true si id_socket pertenece a este mismo grupo local (es vecino).
   */
  boolean esVecino (ClusterMemberID id_socket)
  {
   return this.treeMapID_Socket.containsKey (id_socket);
  }

  //==========================================================================
  /**
   * Devuelve el nï¿½mero de vecinos de este Grupo Local.
   */
  int numeroVecinos()
  {
   // El nï¿½mero de socket pertenecientes al grupo local menos 1 correspondiente
   // a este socket.
   return this.treeMapID_Socket.size()-1;
  }

  //==========================================================================
  /**
   * Devuelve true si idglPadre es padre jerï¿½rquico para idglFuente.
   */
  boolean esPadre(ClusterGroupID idglPadre,ClusterGroupID idglFuente)
  {
    if ((idglPadre==null)||(idglFuente==null))
     return false;

    //Comprobar si estamos en la cache para ese idglFuente....
    if(this.cachePadres.containsKey(idglFuente))
    {
      TreeMap treemap = (TreeMap) this.cachePadres.get(idglFuente);
      if (treemap.containsKey(idglPadre))
        return true;
    }

    else
    {
      //Comprobar obteniendo los padres para ese idglFuente..
      TreeMap treemap = (TreeMap) this.getCGPadres(idglFuente);
      if (treemap.containsKey(idglPadre))
        return true;
    }

    return false;
  }

  //==========================================================================
  /**
   * Devuelve true si idglHijo es hijo jerï¿½rquico para idglFuente.
   */
  boolean esHijo (ClusterGroupID idglHijo,ClusterGroupID idglFuente)
  {
   if ((idglHijo==null)||(idglFuente==null))
     return false;

    //Comprobar si estamos en la cache para ese idglFuente....
    if(this.cacheHijos.containsKey(idglFuente))
    {
      TreeMap treemap = (TreeMap) this.cacheHijos.get(idglFuente);
      if (treemap.containsKey(idglHijo))
        return true;
    }

    else
    {
      //Comprobar obteniendo los hijos para ese idglFuente..
      TreeMap treemap = (TreeMap) this.getCGHijos(idglFuente);
      if (treemap.containsKey(idglHijo))
        return true;
    }

   return false;
  }



  //==========================================================================
  /**
   * Estado NULO de la mï¿½quina de estados CGL.<br>
   */
  private void maquinaEstadoCGL_NULO()
  {
    //
    // Si hay Datos en el vector, quitarlos.
    //

    if (socketClusterNetImp.getVectorRegistroCGL().size() != 0)
    {
      this.socketClusterNetImp.getVectorRegistroCGL().clear();
    }

    this.socketClusterNetImp.getTemporizador().sleep(1000);
  }

  //==========================================================================
  /**
   * Estado MIEMBRO_GL de la mï¿½quina de estados CGL.<br>
   * Mensajes permitidos:<p>
   * <table>
   * <tr><td> <b>Enviar</b> </td>
   *      <td> <UL>
   *           <IL> GRUPO_LOCAL
   *           <IL> SOCKET_ACEPTADO_EN_GRUPO_LOCAL
   *           </UL>
   *      </td></tr>
   * <tr><td> <b>Recibir SOLO</b></td>
   *      <td> <UL>
   *           <IL> GRUPO_LOCAL
   *           <IL> SOCKET_ACEPTADO_EN_GRUPO_LOCAL
   *           <IL> DEJAR_GRUPO_LOCAL
   *           <IL> ELIMINACION_GRUPO_LOCAL
   *           <IL> BUSCAR_GRUPO_LOCAL
   *           <IL> BUSCAR_GRUPO_LOCAL_VECINO
   *           <IL> UNIRSE_A_GRUPO_LOCAL
   *           </UL>
   *      </td></tr>
   * </table>
   */
  private void maquinaEstadoCGL_MIEMBRO_GL()
  {

    //Notificaciones...
    this.notificacionesTemporizadores();

    //
    // Obtener registros CGLs.
    //
    if (getRegistroCGL())
    {


      switch(tpduCGL.getSUBTIPO())
      {
        //
        // TPDU  <--  TPDU_CGL_GRUPO_LOCAL
        //
       case ClusterNet.TPDU_CGL_GRUPO_LOCAL:
        procesarTPDU_CGL_GRUPO_LOCAL();
        break;

        //
        // TPDU  <--  TPDU_CGL_GRUPO_LOCAL_VECINO
        //
       case ClusterNet.TPDU_CGL_GRUPO_LOCAL_VECINO:
        procesarTPDU_CGL_GRUPO_LOCAL_VECINO();
        break;


        //
        // TPDU  <--  TPDU_CGL_ELIMINACION_GRUPO_LOCAL
        //
       case ClusterNet.TPDU_CGL_ELIMINACION_GRUPO_LOCAL:
        procesarTPDU_CGL_ELIMINACION_GRUPO_LOCAL();
        break;

        //
        // TPDU  <--  TPDU_CGL_SOCKET_ACEPTADO_EN_GRUPO_LOCAL
        //
       case ClusterNet.TPDU_CGL_SOCKET_ACEPTADO_EN_GRUPO_LOCAL:
        procesarTPDU_CGL_SOCKET_ACEPTADO_EN_GRUPO_LOCAL();
        break;

        //
        // TPDU  <--  TPDU_CGL_DEJAR_GRUPO_LOCAL
        //
       case ClusterNet.TPDU_CGL_DEJAR_GRUPO_LOCAL:
          procesarTPDU_CGL_DEJAR_GRUPO_LOCAL();
        break;

        //
        // TPDU  <--  TPDU_CGL_BUSCAR_GRUPO_LOCAL
        //
       case ClusterNet.TPDU_CGL_BUSCAR_GRUPO_LOCAL:
          procesarTPDU_CGL_BUSCAR_GRUPO_LOCAL();
        break;

        //
        // TPDU  <--  TPDU_CGL_BUSCAR_GRUPO_LOCAL_VECINO
        //
       case ClusterNet.TPDU_CGL_BUSCAR_GRUPO_LOCAL_VECINO:
          procesarTPDU_CGL_BUSCAR_GRUPO_LOCAL_VECINO();
        break;

        //
        // TPDU  <--  TPDU_CGL_UNIRSE_A_GRUPO_LOCAL
        //
       case ClusterNet.TPDU_CGL_UNIRSE_A_GRUPO_LOCAL:
          procesarTPDU_CGL_UNIRSE_A_GRUPO_LOCAL();
        break;


       default:
          break;
      }

    }
  }

  //==========================================================================
  /**
   * Estado BUSCAR_GL de la mï¿½quina de estados CGL.<br>
   * Mensajes permitidos:<p>
   * <table>
   * <tr><td> <b>Enviar</b> </td>
   *      <td> <UL>
   *           <IL> BUSCAR_GRUPO_LOCAL
   *           <IL> UNIRSE_A_GRUPO_LOCAL
   *           </UL>
   *      </td></tr>
   * <tr><td> <b>Recibir SOLO</b></td>
   *      <td> <UL>
   *           <IL> GRUPO_LOCAL
   *           </UL>
   *      </td></tr>
   * </table>
   */
  private void maquinaEstadoCGL_BUSCAR_GL()
  {
    //Notificaciones
    this.notificacionesTemporizadores();

    //
    // Obteber registros CGLs.
    //
    while (getRegistroCGL())
    {

      if(tpduCGL.getSUBTIPO()== ClusterNet.TPDU_CGL_GRUPO_LOCAL)
      {
        //
        // TPDU  <--  TPDU_CGL_GRUPO_LOCAL
        //
        procesarTPDU_CGL_GRUPO_LOCAL();
        return;
      }
    }

    while(bBuscarGL == false)
      return;

    //
    // Verificar Nï¿½ Mï¿½ximo de intentos
    //
    if (N_INTENTOS >= MAX_INTENTOS_BUSQUEDA_GRUPO_LOCAL)
    {
      setEstadoCGL(ClusterNet.ESTADO_CGL_CREAR_GL);
      return;
    }

    //
    // TPDU --> BUSCAR_GRUPO_LOCAL
    //
    enviarTPDU_CGL_BUSCAR_GRUPO_LOCAL();
    bBuscarGL = false;

    //
    // Incrementar el nï¿½mero de intentos de bï¿½squeda y esperar un poco.
    //
    N_INTENTOS++;

    //
    //ClusterTimer de espera...
    //
    this.socketClusterNetImp.getTemporizador().registrarFuncion(T_ESPERA_BUSQUEDA_GL,timerHandler,EVENTO_BUSCAR_GL);
  }

  //==========================================================================
  /**
   * Estado ESPERAR_ACEPTACION_GL de la mï¿½quina de estados CGL.<br>
   * Mensajes permitidos:<p>
   * <table>
   * <tr><td> <b>Enviar</b> </td>
   *      <td> <UL>
   *           <IL> -
   *           </UL>
   *      </td></tr>
   * <tr><td> <b>Recibir SOLO</b></td>
   *      <td> <UL>
   *           <IL> SOCKET_ACEPTADO_EN_GRUPO_LOCAL
   *           </UL>
   *      </td></tr>
   * </table>
   */
  private void maquinaEstadoCGL_ESPERAR_ACEPTACION_GL()
  {
     //Notificaciones
    this.notificacionesTemporizadores();

    //
    // Obteber registro CGL.
    //
    while (getRegistroCGL())
    {

      if( tpduCGL.getSUBTIPO() == ClusterNet.TPDU_CGL_SOCKET_ACEPTADO_EN_GRUPO_LOCAL)
      {
        //
        // TPDU  <--  TPDU_CGL_SOCKET_ACEPTADO_EN_GRUPO_LOCAL
        //
        procesarTPDU_CGL_SOCKET_ACEPTADO_EN_GRUPO_LOCAL();
        return;
       }
    }
  }


  //==========================================================================
  /**
   * Estado CREAR_GL de la mï¿½quina de estados CGL.<br>
   * Mensajes permitidos:<p>
   * <table>
   * <tr><td> <b>Enviar</b> </td>
   *      <td> <UL>
   *           <IL> TPDU_CGL_GRUPO_LOCAL
   *           </UL>
   *      </td></tr>
   * <tr><td> <b>Recibir SOLO</b></td>
   *      <td> <UL>
   *           <IL> -
   *           </UL>
   *      </td></tr>
   * </table>
   */
  private void maquinaEstadoCGL_CREAR_GL()
  {
    final String mn = "CGLThread.maquinaEstadoCGL_CREAR_GL";

    Buffer   buf       = null;

      //
      // Limpiar vector de registros CGL.
      //
      this.socketClusterNetImp.getVectorRegistroCGL().clear();


      //
      // CREAR:
      //  1ï¿½ ClusterGroupID PARA EL GRUPO LOCAL QUE VAMOS A CREAR...
      //  2ï¿½ Establecer nï¿½ mï¿½ximo de sockets en el grupo.
      //  3ï¿½ Crear vectores (IP, IDGLs, ...)
      //  4ï¿½ Aï¿½adirnos al treeMapId_Socket, lï¿½a lista de sockets del grupo
      try
      {
       buf = new Buffer(6);
       buf.addBytes( new Buffer(this.socketClusterNetImp.getAddressLocal().getInetAddress().getAddress()),0,4);
       buf.addShort(this.socketClusterNetImp.getCanalUnicast().getAddressUnicast().getPort(),4);

       this.clusterGroupID = new ClusterGroupID(buf,(byte)0);
       this.N_MAX_SOCKETS_GL = ClusterNet.MAX_SOCKETS_GL;
       this.N_SOCKETS = 1;

       //aï¿½adir clusterGroupID
       addIDGL(this.clusterGroupID,null);

       // Aï¿½adir este socket al vector de ID_SOCKETS del grupo.
       ClusterMemberID id = this.socketClusterNetImp.getID_Socket();
       this.addID_Socket(id);
      }
      catch(ClusterNetInvalidParameterException e)
      {
        Log.log(mn,e.getMessage());
      }
      catch(ClusterNetExcepcion e)
      {
        Log.log(mn,e.getMessage());
      }


      //
      // TPDU --> ClusterNet.TPDU_CGL_GRUPO_LOCAL
      //
      enviarTPDU_CGL_GRUPO_LOCAL((byte)1);


      //Notificar creaciï¿½n...
      this.socketClusterNetImp.sendPTMFEventConexion("CGL: Crear GL ->"+ this.clusterGroupID);


      //
      // CAMBIAR DE ESTADO --> BUSCAR_GL_VECINOS
      //
      this.setEstadoCGL(ClusterNet.ESTADO_CGL_BUSCAR_GL_VECINOS);
  }

  //==========================================================================
  /**
   * Estado BUSCAR_GL_VECINOS de la mï¿½quina de estados CGL.<br>
   * Mensajes permitidos:<p>
   * <table>
   * <tr><td> <b>Enviar</b> </td>
   *      <td> <UL>
   *           <IL> TPDU_CGL_BUSCAR_GRUPO_LOCAL
   *           </UL>
   *      </td></tr>
   * <tr><td> <b>Recibir SOLO</b></td>
   *      <td> <UL>
   *           <IL> TPDU_CGL_BUSCAR_GRUPO_LOCAL
               <IL> TPDU_CGL_BUSCAR_GRUPO_LOCAL_VECINO
   *           <IL> TPDU_CGL_GRUPO_LOCAL
   *           <IL> TPDU_CGL_DEJAR_GRUPO_LOCAL
   *           <IL> TPDU_CGL_ELIMINACION_GRUPO_LOCAL
   *           <IL> TPDU_CGL_SOCKET_ACEPTADO_EN_GRUPO_LOCAL
   *           <IL> TPDU_CGL_UNIRSE_A_GRUPO_LOCAL
   *           </UL>
   *      </td></tr>
   * </table>
   */
  private void maquinaEstadoCGL_BUSCAR_GL_VECINOS()
  {
    this.notificacionesTemporizadores();

    //
    // Obteber registros CGLs.
    //
    while (getRegistroCGL())
    {

      switch(tpduCGL.getSUBTIPO())
      {
        //
        // TPDU  <--  TPDU_CGL_BUSCAR_GRUPO_LOCAL
        //
       case ClusterNet.TPDU_CGL_BUSCAR_GRUPO_LOCAL:
        procesarTPDU_CGL_BUSCAR_GRUPO_LOCAL();
        break;

        //
        // TPDU  <--  TPDU_CGL_BUSCAR_GRUPO_LOCAL
        //
       case ClusterNet.TPDU_CGL_BUSCAR_GRUPO_LOCAL_VECINO:
        procesarTPDU_CGL_BUSCAR_GRUPO_LOCAL_VECINO();
        break;

        //
        // TPDU  <--  TPDU_CGL_GRUPO_LOCAL
        //
       case ClusterNet.TPDU_CGL_GRUPO_LOCAL:
        procesarTPDU_CGL_GRUPO_LOCAL();
        break;

        //
        // TPDU  <--  TPDU_CGL_GRUPO_LOCAL_VECINO
        //
       case ClusterNet.TPDU_CGL_GRUPO_LOCAL_VECINO:
        procesarTPDU_CGL_GRUPO_LOCAL_VECINO();
        break;

        //
        // TPDU  <--  TPDU_CGL_ELIMINACION_GRUPO_LOCAL
        //
       case ClusterNet.TPDU_CGL_ELIMINACION_GRUPO_LOCAL:
        procesarTPDU_CGL_ELIMINACION_GRUPO_LOCAL();
        break;

        //
        // TPDU  <--  TPDU_CGL_SOCKET_ACEPTADO_EN_GRUPO_LOCAL
        //
       case ClusterNet.TPDU_CGL_SOCKET_ACEPTADO_EN_GRUPO_LOCAL:
        procesarTPDU_CGL_SOCKET_ACEPTADO_EN_GRUPO_LOCAL();
        break;

        //
        // TPDU  <--  TPDU_CGL_DEJAR_GRUPO_LOCAL
        //
       case ClusterNet.TPDU_CGL_DEJAR_GRUPO_LOCAL:
          procesarTPDU_CGL_DEJAR_GRUPO_LOCAL();
        break;

        //
        // TPDU  <--  UNIRSE_A_GRUPO_LOCAL
        //
       case ClusterNet.TPDU_CGL_UNIRSE_A_GRUPO_LOCAL:
          procesarTPDU_CGL_UNIRSE_A_GRUPO_LOCAL();
        break;

       default:
          break;
      }

    }


    if(bBuscarGLVecino == false)
      return;

    //
    // CAMBIO A ESTADO CGL_MIEMBRO_GL SI:
    //  1. YA SE HA RECIBIDO ALGUN TPDU GRUPO_LOCAL
    //  2. SE HA EXCEDIDO EL Nï¿½ MAXIMO DE INTENTOS
    //
    if ( this.TTL_BUSCAR_GRUPO_LOCAL_VECINO > this.socketClusterNetImp.getTTLSesion() /*|| this.treeMapIDGLPadresPotenciales.size()>0*/)
    {
      //
      // Cambiar de estado,
      // NOTA: NO SE MANDA UN MENSAJE GRUPO_LOCAL PORQUE
      // NO SE HA ENCONTRADO NINGï¿½N socket VECINO.
      //
      setEstadoCGL(ClusterNet.ESTADO_CGL_MIEMBRO_GL);
      return;
    }


    bBuscarGLVecino = false;
    //
    // TPDU --> BUSCAR_GRUPO_LOCAL_VECINO
    //
    enviarTPDU_CGL_BUSCAR_GRUPO_LOCAL_VECINO();


    //
    //ClusterTimer de espera ...
    //
    this.socketClusterNetImp.getTemporizador().registrarFuncion(T_ESPERA_BUSQUEDA_GL_VECINO+(this.TTL*100),timerHandler,EVENTO_BUSCAR_GL_VECINO);
  }

  //==========================================================================
  /**
   * Estado DEJAR_GL de la mï¿½quina de estados CGL.<br>
   * Mensajes permitidos:<p>
   * <table>
   * <tr><td> <b>Enviar</b> </td>
   *      <td> <UL>
   *           <IL> DEJAR_GRUPO_LOCAL
   *           </UL>
   *      </td></tr>
   * <tr><td> <b>Recibir SOLO</b></td>
   *      <td> <UL>
   *           <IL> -
   *           </UL>
   *      </td></tr>
   * </table>
   */
  private void maquinaEstadoCGL_DEJAR_GL()
  {

   //
   // Decrementar el nï¿½mero de sockets en el GL...
   //
   this.N_SOCKETS--;

   //
   // TPDU --> DEJAR_GRUPO_LOCAL
   //
   enviarTPDU_CGL_DEJAR_GRUPO_LOCAL(this.socketClusterNetImp.getID_Socket());

   if(this.N_SOCKETS == 0)
   {
     //
     // TPDU --> ELIMINACION_GRUPO_LOCAL
     //
     enviarTPDU_CGL_ELIMINACION_GRUPO_LOCAL();
   }

   //
   // Desactivar grupo Multicast
   this.socketClusterNetImp.setGrupoMcastActivo(false);

   //
   // CAMBIAR DE ESTADO --> NULO
   //
   this.setEstadoCGL(ClusterNet.ESTADO_CGL_NULO);

  }

  //==========================================================================
  /**
   * Estado MONITOR de la mï¿½quina de estados CGL.<br>
   * En este estado el socket se encarga de monotorizar los mensajes CGL
   * y de imprimirlos.
   */
  private void maquinaEstadoCGL_MONITOR()
  {

    //Log.debug(Log.CGL,"CGL ","MAQUINA ESTADO CGL MONITOR");
    //
    // Obteber registro CGL.
    //
    while (getRegistroCGL())
    {
      Log.log("TPDUCGL "+src.toString(),"\n"+tpduCGL);
    }


  }


  //==========================================================================
  /**
   * Este mï¿½todo sincronizado establece el valor del atributo estadoCGL.<br>
   * @param estadoCGL Nuevo estado de la mï¿½quina de estados CGL.
   */
  synchronized void setEstadoCGL (int estadoCGL)
  {
     String mn = "CGLThread.setEstadoCGL";
     //
     // CAMBIAR AQUï¿½ LOS ESTADOS DE LAS VARIABLES DEPENDIENTES DEL ESTADO DE LA
     // Mï¿½QUINA.
     //
     if(estadoCGL == this.estadoCGL)
      return;

     switch(estadoCGL)
     {
        case ClusterNet.ESTADO_CGL_BUSCAR_GL:
            this.TTL = 0;
            this.N_INTENTOS = 0;
            this.estadoCGL = estadoCGL;
            this.bBuscarGL = true;
            this.bBuscarGLVecino = false;
            this.socketClusterNetImp.setGrupoMcastActivo(false);
            Log.debug(Log.CGL,mn,"Estado CGL: ClusterNet.ESTADO_CGL_BUSCAR_GL");
             //Log.log("CGLThread","Estado CGL: ClusterNet.ESTADO_CGL_BUSCAR_GL");
             this.socketClusterNetImp.sendPTMFEventConexion("CGL: Buscar Grupos Locales ...");
            break;
        case ClusterNet.ESTADO_CGL_ESPERAR_ACEPTACION_GL:
            this.N_INTENTOS=0;
            this.socketClusterNetImp.setGrupoMcastActivo(false);
            this.estadoCGL = estadoCGL;
            Log.debug(Log.CGL,mn,"Estado CGL: ClusterNet.ESTADO_CGL_ESPERAR_ACEPTACION_GL");
            //Log.log("CGLThread","Estado CGL: ClusterNet.ESTADO_CGL_ESPERAR_ACEPTACION_GL");

            this.socketClusterNetImp.sendPTMFEventConexion("CGL: Esperar aceptaciï¿½n en Grupo Local: "+ this.clusterGroupID);


            break;

        case ClusterNet.ESTADO_CGL_MIEMBRO_GL:
            this.estadoCGL = estadoCGL;
            this.bNotificarGL = false;
            this.bAceptarSocket = false;
            this.bNotificarGLVecinos = false;

            //
            //Activar grupo Multicast
            this.socketClusterNetImp.setGrupoMcastActivo(true);

            Log.debug(Log.CGL,mn,"Estado CGL: ClusterNet.ESTADO_MIEMBRO_GL");
            Log.debug(Log.CGL,mn,"ClusterGroupID: " +  this.clusterGroupID);
            this.socketClusterNetImp.sendPTMFEventConexion("CGL: Miembro Grupo Local: "+  this.clusterGroupID);

            //
            // LIBERAR THREAD DE LA APLICACIï¿½Nï¿½ï¿½ï¿½ï¿½ï¿½ï¿½
            //
            this.socketClusterNetImp.getSemaforoAplicacion().up();

            break;

        case ClusterNet.ESTADO_CGL_CREAR_GL:
            this.estadoCGL = estadoCGL;
            this.socketClusterNetImp.setGrupoMcastActivo(false);
            Log.debug(Log.CGL,mn,"Estado CGL: ClusterNet.ESTADO_CGL_CREAR_GL");
            //Log.log("CGLThread","Estado CGL: ClusterNet.ESTADO_CGL_CREAR_GL");


            break;

        case ClusterNet.ESTADO_CGL_BUSCAR_GL_VECINOS:
            this.estadoCGL = estadoCGL;
            this.TTL = 0;
            this.bBuscarGLVecino = true;
            Log.debug(Log.CGL,mn,"Estado CGL: ClusterNet.ESTADO_CGL_BUSCAR_GL_VECINOS");
            //Log.log("CGLThread","Estado CGL: ClusterNet.ESTADO_CGL_BUSCAR_GL_VECINOS");

            this.socketClusterNetImp.sendPTMFEventConexion("CGL: Buscar Grupos Locales Vecinos ...");


            break;

        case ClusterNet.ESTADO_CGL_MONITOR:
            this.estadoCGL = estadoCGL;
            this.socketClusterNetImp.setGrupoMcastActivo(false);
            Log.debug(Log.CGL,mn,"Estado CGL: ClusterNet.ESTADO_CGL_MONITOR");
            break;

        case ClusterNet.ESTADO_CGL_NULO:


            //
            // Limpiar variables a valores por defectos.
            //
            this.treeMapID_Socket = null;
            this.treeMapIDGLs = null;
            this.clusterGroupID = null;
            this.N_SOCKETS=0;
            this.N_MAX_SOCKETS_GL=0;
            this.TTL = 0;
            this.N_INTENTOS = 0;
            this.estadoCGL = estadoCGL;
            this.socketClusterNetImp.setGrupoMcastActivo(false);

            //
            // LIBERAR THREAD DE LA APLICACIï¿½Nï¿½ï¿½ï¿½ï¿½ï¿½ï¿½
            //
            this.socketClusterNetImp.getSemaforoAplicacion().up();


            Log.debug(Log.CGL,mn,"Estado CGL: ClusterNet.ESTADO_CGL_NULO");
            //Log.log("CGLThread","Estado CGL: ClusterNet.ESTADO_CGL_NULO");

            break;

        case ClusterNet.ESTADO_CGL_DEJAR_GL :
           this.estadoCGL = estadoCGL;
           Log.debug(Log.CGL,mn,"Estado CGL: ClusterNet.ESTADO_CGL_DEJAR_GL");
           //Log.log("CGLThread","Estado CGL: ClusterNet.ESTADO_CGL_DEJAR_GL");
            this.socketClusterNetImp.sendPTMFEventConexion("CGL: Dejar Grupo Local ...");

           break;
        default:

          Log.log("ERROR FATAL (CGL)","ESTADO CGL INCORRECTO");
          Log.exit(-1);
     }
  }

  //==========================================================================
  /**
   * Este mï¿½todo sincronizado devuelve el valor del atributo estadoCGL.<br>
   * @return el estado de la mï¿½quina de estados CGL.
   */
  synchronized int getEstadoCGL ()
  {
   return estadoCGL;
  }

  //==========================================================================
  /**
   * Este mï¿½todo devuelve el ClusterGroupID de este socket.
   * @return Objeto ClusterGroupID
   */
  ClusterGroupID getIDGL ()
  {
   return this.clusterGroupID;
  }



  //==========================================================================
  /**
   * Este mï¿½todo obtiene un registro CGL del vector vectorRegistroCGL
   *  de la clase ClusterNet, LOS DATOS DEL REGISTRO SON ALMACENADOS
   * EN LAS VARIABLES tpduCGL y src DE LA CLASE.
   * @return true si se obtuvo un registro CGL y false en caso contrario.
   */
  private boolean getRegistroCGL()
  {
    final String mn = "CGLThread.getRegistroCGL(TPDUCGL,Address)";
    RegistroCGL registroCGL = null;

    //
    // Obtener el registro CGL del vectorCGL de la clase.
    //

    if (socketClusterNetImp.getVectorRegistroCGL().size() != 0)
    {
      registroCGL = (RegistroCGL) this.socketClusterNetImp.getVectorRegistroCGL().remove(0);

      tpduCGL = registroCGL.tpduCGL;
      src = registroCGL.src;

      //COMPRABAR QUE EL PAQUETE NO ES ENVIADO POR NOSOTROS
      // OJO, Comprueba con una direciï¿½n local, pueden surgir problemas
      // si tenems varias interfaces.
      if( (this.getEstadoCGL()!= ClusterNet.ESTADO_CGL_MONITOR)
              &&
          (this.socketClusterNetImp.getAddressLocal().getInetAddress().equals(src.getInetAddress()))
              && (tpduCGL.getPuertoUnicast()==this.socketClusterNetImp.getAddressLocal().getPort()))
      {
      Log.debug(Log.TPDU_CGL,mn,"!cgl ");
        return false;
      }


      //Log.debug(Log.TPDU_CGL,mn,"Registro CGL Recibido:\nFuente-->"+src.toString()+"\nTPDU-->"+tpduCGL.toString());
      return true;
    }
    else
     return false;
  }


  //==========================================================================
  /**
   * procesarTPDU_CGL_SOCKET_ACEPTADO_EN_GRUPO_LOCAL. Procesa una cabecera de un TPDU
   *  SOCKET_ACEPTADO_EN_GRUPO_LOCAL
   */
  private void procesarTPDU_CGL_SOCKET_ACEPTADO_EN_GRUPO_LOCAL()
  {

    String mn = "CGLThread.procesarTPDU_CGL_SOCKET_ACEPTADO_EN_GRUPO_LOCAL";

    Log.debug(Log.CGL,mn,"TPDUCGL <-- SOCKET_ACEPTADO_EN_GRUPO_LOCAL "+tpduCGL.getIDGL() );

    if (! tpduCGL.getIDGL().equals(this.clusterGroupID))
        return;

    if(getEstadoCGL() == ClusterNet.ESTADO_CGL_ESPERAR_ACEPTACION_GL)
    {
        if (tpduCGL.getID_SOCKET() == null || !tpduCGL.getID_SOCKET().equals(this.socketClusterNetImp.getID_Socket()))
          return;

        //Log.log("ID_SOCKET de ACEPTADO EN GRUPO LOCAL",""+tpduCGL.getID_SOCKET());
        //Log.log("ID_SOCKET LOCAL",""+this.socketPTMFImp.getID_Socket());

        if(!tpduCGL.getID_SOCKET().equals(this.socketClusterNetImp.getID_Socket()))
          return;

        //1. Cancelar temporizador de bï¿½squeda de grupo local y time out
        //2.  y el flag correspondiente de notificaciï¿½n,
        //3.  y quitar el ClusterMemberID de la cola de aceptaciï¿½n
        this.socketClusterNetImp.getTemporizador().cancelarFuncion(this.timerHandler,EVENTO_BUSCAR_GL);
        this.socketClusterNetImp.getTemporizador().cancelarFuncion(this.timerHandler,EVENTO_TIME_OUT);

        this.bAceptarSocket= false;
        this.colaAceptacionID_SOCKET.remove(tpduCGL.getID_SOCKET());

        this.addID_Socket(tpduCGL.getID_SOCKET());

        //Log.log("1. tREEMAP id_sOCKETS "+tpduCGL.getTreeMapID_SOCKET() ,"");

        if (tpduCGL.getTreeMapID_SOCKET() != null)
        {
             this.addID_Socket(tpduCGL.getTreeMapID_SOCKET());
        }
        this.N_SOCKETS = tpduCGL.getN_SOCKETS();

        //Log.log("2. tREEMAP id_sOCKETS "+this.treeMapID_Socket,"");

        //Socket aceptado
        this.socketClusterNetImp.sendPTMFEventConexion("CGL: ClusterNet aceptado en Grupo Local: "+ this.clusterGroupID);


        //
        // CAMBIAR DE ESTADO --> MIEMBRO_GRUPO_LOCAL
        //
        this.setEstadoCGL(ClusterNet.ESTADO_CGL_MIEMBRO_GL);
    }
    else //ESTADo --> MIEMBRO_GRUPO_LOCAL
    {
        if(tpduCGL.getN_IDS() < 1)
        {
          Log.log("CGLThread","Error en el TPDUCGL SOCKET_ACEPTADO_EN_GRUPO_LOCAL, se espera un N_IDS con valor 1.");
          return;
        }

        //
        // Incrementar nï¿½mero de sockets y almacenarlo
        //
        if (colaAceptacionID_SOCKET.contains(tpduCGL.getID_SOCKET()))
        {
            colaAceptacionID_SOCKET.remove(tpduCGL.getID_SOCKET());
            this.socketClusterNetImp.getTemporizador().cancelarFuncion(this.timerHandler,EVENTO_ACEPTAR_SOCKET);
            bAceptarSocket = false;
        }


        if( ! this.treeMapID_Socket.containsKey(tpduCGL.getID_SOCKET()))
        {
          // SI NO TENEMOS EL NUEVO SOCKET EN EL TREEMAP...
          this.addID_Socket(tpduCGL.getID_SOCKET());

          if (tpduCGL.getTreeMapID_SOCKET() != null)
             this.addID_Socket(tpduCGL.getTreeMapID_SOCKET());
          this.N_SOCKETS ++;
        }
        else
        {
           // SI YA LO TENï¿½AMOS....
           if (tpduCGL.getTreeMapID_SOCKET() != null)
             this.addID_Socket(tpduCGL.getTreeMapID_SOCKET());
        }

        if (this.N_SOCKETS != tpduCGL.getN_SOCKETS())
        {
         Log.log("CGLThread","Nï¿½mero de sockets en el grupo inconsistente. Recibido: "+tpduCGL.getN_SOCKETS()+" Esperado: "+this.N_SOCKETS+" AJUSTADO.");
         this.N_SOCKETS = tpduCGL.getN_SOCKETS();
        }
    }
  }

  //==========================================================================
  /**
   * procesarTPDU_CGL_UNIRSE_A_GRUPO_LOCAL. Procesa una cabecera de un TPDU
   *  UNIRSE_A_GRUPO_LOCAL
   */

  private void procesarTPDU_CGL_UNIRSE_A_GRUPO_LOCAL()
  {
    String mn = "CGLThread.procesarTPDU_CGL_UNIRSE_A_GRUPO_LOCAL";
    Log.debug(Log.CGL,mn,"TPDUCGL <-- UNIRSE_A_GRUPO_LOCAL"+"\n"+tpduCGL.getIDGL());

    if (!tpduCGL.getIDGL().equals(this.clusterGroupID))
        return;

    if (!tpduCGL.getFlagIP() )
        return;

    if(tpduCGL.getN_IDS() > 1)
    {
          Log.log("CGLThread","Error en el TPDUCGL UNIRSE_A_GRUPO_LOCAL, se espera un N_IDS con valor 1.");
          return;
    }


    //Log.debug(Log.CGL,mn,"this.N_SOCKETS "+this.N_SOCKETS);
    //Log.debug(Log.CGL,mn,"this.MAX_SOCKETS_GL "+this.N_MAX_SOCKETS_GL);

    if (this.N_SOCKETS < this.N_MAX_SOCKETS_GL)
    {
     if (tpduCGL.getID_SOCKET() == null)
     {
      Log.log("CGLThread","TPDUCGL UNIRSE_A_GRUPO_LOCAL sin identificador ID_SOCKET. NO SE PUEDE PROCESAR.");
      return;
     }

     //Aï¿½adir el socket a la cola de aceptaciï¿½n y lanzar el temporizador....
     this.colaAceptacionID_SOCKET.add(tpduCGL.getID_SOCKET());

     this.socketClusterNetImp.getTemporizador().registrarFuncion(
      (random.nextInt(this.T_RETRASO_NOTIFICACION_SOCKET_ACEPTADO -T_BASE)+T_BASE) % this.T_RETRASO_NOTIFICACION_SOCKET_ACEPTADO ,
        this.timerHandler,this.EVENTO_ACEPTAR_SOCKET );
    }


  }

  //==========================================================================
  /**
   * procesarTPDU_CGL_DEJAR_GRUPO_LOCAL. Procesa una cabecera de un TPDU
   *  DEJAR_GRUPO_LOCAL
   */

  private void procesarTPDU_CGL_DEJAR_GRUPO_LOCAL()
  {
        String mn = "CGLThread.procesarTPDU_CGL_DEJAR_GRUPO_LOCAL";

        Log.debug(Log.CGL,mn,"TPDUCGL <-- DEJAR_GRUPO_LOCAL "+tpduCGL.getIDGL());

        if (! tpduCGL.getIDGL().equals(this.clusterGroupID))
          return;

        if (!tpduCGL.getFlagIP())
           return;

        //Comprobar que no somos "nozotros"...
        if(tpduCGL.getID_SOCKET().equals(this.socketClusterNetImp.getID_Socket()))
        {
           //
           // eeeh, QUE SOY YO...
           //

           //
           // TPDU --> GRUPO_LOCAL
           //
           enviarTPDU_CGL_GRUPO_LOCAL((byte)this.socketClusterNetImp.getTTLSesion());
        }

        if(tpduCGL.getN_IDS() != 1)
        {
          Log.log("CGLThread","Error en el TPDUCGL DEJAR_GRUPO_LOCAL, se espera un N_IDS con valor 1.");
          return;
        }

        //
        // 1. Quitar el socket del grupo local
        this.removeID_SOCKET(tpduCGL.getID_SOCKET());

  }

  //==========================================================================
  /**
   * procesarTPDU_CGL_ELIMINACION_GRUPO_LOCAL. Procesa una cabecera de un TPDU
   *  ELIMINACION_GRUPO_LOCAL
   */

  private void procesarTPDU_CGL_ELIMINACION_GRUPO_LOCAL()
  {
        String mn = "CGLThread.procesarTPDU_CGL_ELIMINACION_GRUPO_LOCAL";

        Log.debug(Log.CGL,mn,"TPDUCGL <-- ELIMINACION_GRUPO_LOCAL"+"\n"+tpduCGL.getIDGL());

        if (! tpduCGL.getIDGL().equals(this.clusterGroupID))
        {
          //
          //Quitar clusterGroupID del grupo local eliminado
          //
          this.removeIDGL(tpduCGL.getIDGL());
          comprobarCache(tpduCGL.getIDGL());
        }
        else
        {

         //
         // eeeh, QUE Aï¿½N ESTOY YO...
         //

         //
         // TPDU --> GRUPO_LOCAL
         //
         enviarTPDU_CGL_GRUPO_LOCAL((byte)this.socketClusterNetImp.getTTLSesion());
        }
  }



 //==========================================================================
  /**
   * procesarTPDU_CGL_BUSCAR_GRUPO_LOCAL_VECINO. Procesa una cabecera de un TPDU

   *  BUSCAR_GRUPO_LOCAL_VECINO
   */

  private void procesarTPDU_CGL_BUSCAR_GRUPO_LOCAL_VECINO()
  {
     String mn = "CGLThread.procesarTPDU_CGL_ELIMINACION_GRUPO_LOCAL";

     Log.debug(Log.CGL,mn,"TPDUCGL <-- BUSCAR_GRUPO_LOCAL_VECINO "+tpduCGL.getIDGL());
    //Log.log("CGLThread","TPDUCGL <-- BUSCAR_GRUPO_LOCAL_VECINO "+tpduCGL.getIDGL());

    if(!tpduCGL.getIDGL().equals(this.clusterGroupID))
    {
     //Almacenar el ClusterGroupID Vecino...
     addIDGL();

     //Recordar el ClusterGroupID (y el TTL especialmente)
     this.treeMapRespuestaGLVecinos.put(tpduCGL.getIDGL(),null);

     if (!bLanzadoTemporizadorNotificacionGLVecino)
     {
       //Lanzar this.socketPTMFImp.getTemporizador()...
       this.socketClusterNetImp.getTemporizador().registrarFuncion( (random.nextInt(this.T_RETRASO_NOTIFICACION_GL_VECINO -T_BASE)+100) % this.T_RETRASO_NOTIFICACION_GL_VECINO ,
          this.timerHandler,this.EVENTO_NOTIFICAR_GL_VECINOS);
       bLanzadoTemporizadorNotificacionGLVecino = true;
     }

    }
  }

  //==========================================================================
  /**
   * procesarTPDU_CGL_BUSCAR_GRUPO_LOCAL. Procesa una cabecera de un TPDU
   *  BUSCAR_GRUPO_LOCAL
   */
  private void procesarTPDU_CGL_BUSCAR_GRUPO_LOCAL()
  {
     String mn = "CGLThread.procesarTPDU_CGL_BUSCAR_GRUPO_LOCAL";

     Log.debug(Log.CGL,mn,"TPDUCGL <-- BUSCAR_GRUPO_LOCAL "+tpduCGL.getIDGL());
     // Log.log("CGLThread","TPDUCGL <-- BUSCAR_GRUPO_LOCAL "+tpduCGL.getIDGL());

    //Log.log("this.N_SOCKETS "+this.N_SOCKETS,"");
    //Log.log("ClusterNet.MAX_SOCKETS_GL "+ClusterNet.MAX_SOCKETS_GL,"");

    if(this.N_SOCKETS < ClusterNet.MAX_SOCKETS_GL)
    {
      if(!bLanzadoTemporizadorNotificacionGL)
      {
         //Lanzar temporizador de notificaciï¿½n de GRUPO LOCAL
         // ï¿½ï¿½ SOLO SI HAY ESPACIO PARA OTRO ID_SOCKET, SINO NOï¿½ï¿½ï¿½
        this.socketClusterNetImp.getTemporizador().registrarFuncion( (random.nextInt(this.T_RETRASO_NOTIFICACION_GL-T_BASE)+5000) % this.T_RETRASO_NOTIFICACION_GL ,
            this.timerHandler,this.EVENTO_NOTIFICAR_GL);

        bLanzadoTemporizadorNotificacionGL = true;
        this.TTL_Notificacion_GL = tpduCGL.getIDGL().TTL;
      }
      //Comprobar si el TTL sel mensaje es mayor que el actual
     else if(tpduCGL.getTTL() > this.TTL_Notificacion_GL)
     {
        this.TTL_Notificacion_GL = tpduCGL.getIDGL().TTL;
     }

    }
  }

  //==========================================================================
  /**

   * procesarTPDU_CGL_BUSCAR_GL_PARA_EMISOR. Procesa una cabecera de un TPDU
   *  BUSCAR_GL_PARA_EMISOR
   */
  private void procesarTPDU_CGL_BUSCAR_GL_PARA_EMISOR()
  {
     String mn = "CGLThread.procesarTPDU_CGL_BUSCAR_GL_PARA_EMISOR";

     Log.debug(Log.CGL,mn,"TPDUCGL <-- BUSCAR_GL_PARA_EMISOR "+tpduCGL.getIDGL());

    if ((tpduCGL.getTreeMapIDGL() == null) || (tpduCGL.getTreeMapIDGL().size() <= 0))
      return;

    //Si no es de nuestro Grupo Local este socket...
    if(!tpduCGL.getIDGL().equals(this.clusterGroupID))
    {
      //Ver si nosotros somos CG para ese ClusterGroupID Emisor...
      if (this.esHijo((ClusterGroupID)tpduCGL.getIDGL_EMISOR(),this.clusterGroupID))
      {
        //Almacenar el IDGL_EMISOR del emisor para el que se busca "Padre" en la lista de respuesta....
        this.treeMapRespuestaBusquedaEmisores.put( tpduCGL.getIDGL_EMISOR(),null);

        //Lanzar temporizador de notificaciï¿½n de GRUPO LOCAL....
        this.socketClusterNetImp.getTemporizador().registrarFuncion( (random.nextInt(this.T_RETRASO_NOTIFICACION_GL_PARA_EMISOR-T_BASE)+T_BASE) % this.T_RETRASO_NOTIFICACION_GL_PARA_EMISOR ,
          this.timerHandler,this.EVENTO_NOTIFICAR_GL_EMISOR);
      }
    }
  }


   //==========================================================================
  /**
   * procesarTPDU_CGL_GL_PARA_EMISOR. Procesa una cabecera de un TPDU
   *  GL_PARA_EMISOR
   */
  private void procesarTPDU_CGL_GL_PARA_EMISOR()
  {
    final String mn = "DataThread.procesarTPDUCGL_GL_PARA_EMISOR()";
    Log.debug(Log.CGL,mn,"TPDUCGL <-- GL_PARA_EMISOR "+tpduCGL.getIDGL());

    if ((tpduCGL.getTreeMapIDGL() == null) || (tpduCGL.getTreeMapIDGL().size() <= 0))
      return;


    if((!tpduCGL.getIDGL().equals(this.clusterGroupID)) && (this.treeMapBusquedaEmisores.containsKey(tpduCGL.getIDGL_EMISOR())))
    {
      // eliminar ClusterGroupID ....
      this.treeMapBusquedaEmisores.remove(tpduCGL.getIDGL_EMISOR());

      //Guardar ClusterGroupID...
      this.addIDGL();

      //Despertar DataThread...
      //Log.log (mn,"POR LO MENOS ME HAN DESPERTADO.");
      this.semaforoDatosThread.up();

      if (this.treeMapBusquedaEmisores.size() <= 0)
      {
        //Cancelar temporizador de busqueda....
        this.socketClusterNetImp.getTemporizador().cancelarFuncion(this.timerHandler,this.EVENTO_BUSCAR_GL_EMISOR);
      }
    }

    if(tpduCGL.getIDGL().equals(this.clusterGroupID))
    {
      //Otro socket ha notificado el mensaje....

      //Cancelar temporizador de notificaciï¿½n....
      this.socketClusterNetImp.getTemporizador().cancelarFuncion(this.timerHandler,this.EVENTO_NOTIFICAR_GL_EMISOR);

      //Si la respuesta se ha dado para un ClusterGroupID del que tambiï¿½n nosotros debemos
      // dar respuesta, eliminar nuestra respuesta....
      this.treeMapRespuestaBusquedaEmisores.remove( tpduCGL.getIDGL_EMISOR());
    }

  }



  //==========================================================================
  /**
   * ProcesarTPDU_CGL_GRUPO_LOCAL. Procesa una cabecera de un TPDU
   *  GRUPO_LOCAL.
   */
  private void procesarTPDU_CGL_GRUPO_LOCAL()
  {
      String mn = "CGLThread.procesarTPDU_CGL_GRUPO_LOCAL";

     Log.debug(Log.CGL,mn,"TPDUCGL <-- GRUPO_LOCAL"+"\n"+tpduCGL.getIDGL());
     Log.debug(Log.CGL,mn,"N_Sockets: "+tpduCGL.getN_SOCKETS());
     Log.debug(Log.CGL,mn,"N_Max_Sockets: "+tpduCGL.getN_MAX_SOCKETS());
   
     //Log.log("CGLThread","TPDUCGL <-- GRUPO_LOCAL: "+tpduCGL.getIDGL());

     //DEPURACION
     if(tpduCGL.getTreeMapIDGL()!= null && tpduCGL.getTreeMapIDGL().size() > 0)
      {
        Iterator iterator = tpduCGL.getTreeMapIDGL().keySet().iterator();
        while(iterator.hasNext())
        {
          ClusterGroupID clusterGroupID = (ClusterGroupID) iterator.next();
          Log.debug(Log.CGL,mn,"ClusterGroupID <--- "+clusterGroupID);
        }
      }

     if(this.getEstadoCGL() == ClusterNet.ESTADO_CGL_BUSCAR_GL)
     {
        //Almacenar el ClusterGroupID y la lista...
        addIDGL();

        //Comprobar que nos podemos unir al grupo...
        if (tpduCGL.getN_SOCKETS() >= tpduCGL.getN_MAX_SOCKETS())
          return;

        //
        // Almacenar datos del GL
        //
        this.clusterGroupID = tpduCGL.getIDGL();
        this.N_MAX_SOCKETS_GL = tpduCGL.getN_MAX_SOCKETS();
        this.N_SOCKETS = tpduCGL.getN_SOCKETS();


        //
        // TPDU --> UNIRSE_A_GRUPO_LOCAL
        //
        enviarTPDU_CGL_UNIRSE_A_GRUPO_LOCAL();

        //
        // CAMBIAR DE ESTADO --> ESPERAR_ACEPTACION
        //
        this.setEstadoCGL(ClusterNet.ESTADO_CGL_ESPERAR_ACEPTACION_GL);

        //
        // Lanzar temporizador time out.....
        this.socketClusterNetImp.getTemporizador().registrarFuncion(T_ESPERA_ACEPTACION_EN_GRUPO_LOCAL,timerHandler,EVENTO_TIME_OUT);


     }
     else if(this.getEstadoCGL() == ClusterNet.ESTADO_CGL_MIEMBRO_GL)// ESTADO MIEMBRO_GL
     {
        if(tpduCGL.getIDGL().equals(this.clusterGroupID))
        {
            // Aqui se entra si el emisor del TPDU es de nuestro GRUPO_LOCAL

            //
            // Si hemos recibido una notificaciï¿½n de nuestro grupo local
            // significa:

            // 1ï¿½ --> que otro socket ha lanzado el TPDU GRUPO_LOCAL
            //        ante la recepciï¿½n de un mensaje BUSCAR_GRUPO_LOCAL


            this.socketClusterNetImp.getTemporizador().cancelarFuncion(this.timerHandler,this.EVENTO_NOTIFICAR_GL);
            this.bNotificarGL= false;

            //Eliminar el ClusterGroupID....
            this.treeMapRespuestaGLVecinos.remove(tpduCGL.getIDGL());
        }
        else
        {
           //1. Cancelar temporizador de bï¿½squeda de grupo local
           //2.  y el flag correspondiente de notificaciï¿½n,
           this.socketClusterNetImp.getTemporizador().cancelarFuncion(this.timerHandler,EVENTO_BUSCAR_GL);

           //Eliminar el ClusterGroupID.... (AQUï¿½ por si acaso)
           this.treeMapRespuestaGLVecinos.remove(tpduCGL.getIDGL());

           //Almacenar el ClusterGroupID Padre Potencial
           addIDGL();

        }
     }
  }

 //==========================================================================
  /**
   * ProcesarTPDU_CGL_GRUPO_LOCAL_VECINO. Procesa una cabecera de un TPDU
   *  GRUPO_LOCAL_VECINO.
   */
  private void procesarTPDU_CGL_GRUPO_LOCAL_VECINO()
  {
     String mn = "CGLThread.procesarTPDU_CGL_GRUPO_LOCAL_VECINO";

     Log.debug(Log.CGL,mn,"TPDUCGL <-- GRUPO_LOCAL"+"\n"+tpduCGL.getIDGL());
     //Log.log("CGLThread","TPDUCGL <-- GRUPO_LOCAL_VECINO: "+tpduCGL.getIDGL());

     //DEPURACION
     if(tpduCGL.getIDGL_EMISOR()!=null)
     {
          Log.debug(Log.CGL,mn,"IDGL_EMISOR <--- "+tpduCGL.getIDGL_EMISOR());
     }
     if(tpduCGL.getTreeMapIDGL()!= null && tpduCGL.getTreeMapIDGL().size() > 0)
     {
        Iterator iterator = tpduCGL.getTreeMapIDGL().keySet().iterator();

        while(iterator.hasNext())
        {
          ClusterGroupID clusterGroupID = (ClusterGroupID) iterator.next();
          Log.debug(Log.CGL,mn,"ClusterGroupID <--- "+clusterGroupID);
        }
     }
     //FIN_DEPURACION

     this.socketClusterNetImp.sendPTMFEventConexion("CGL: GL Vecino: "+ tpduCGL.getIDGL());

     if(this.getEstadoCGL() == ClusterNet.ESTADO_CGL_BUSCAR_GL_VECINOS)
     {
        //Comprobar si es para nosotros...
        if(this.clusterGroupID != null && tpduCGL.getIDGL_EMISOR().equals(this.clusterGroupID))
        {
           //
           // Cancelar ClusterTimer
           //
           this.socketClusterNetImp.getTemporizador().cancelarFuncion(this.timerHandler,this.EVENTO_BUSCAR_GL_VECINO);

           //Almacenar el ClusterGroupID y la lista de IDGLs...
           addIDGL();

           //
           // CAMBIAR DE ESTADO --> MIEMBRO_GRUPO_LOCAL
           //
           this.setEstadoCGL(ClusterNet.ESTADO_CGL_MIEMBRO_GL);
        }
        else
        {
           //Almacenar el ClusterGroupID y la lista de IDGLs...
           addIDGL();
        }
     }
     else // ESTADO MIEMBRO_GL
     {
        if(tpduCGL.getIDGL().equals(this.clusterGroupID))
        {
            // Aqui se entra si el emisor del TPDU es de nuestro GRUPO_LOCAL

            //
            // Si hemos recibido una notificaciï¿½n de nuestro grupo local
            // significa:

            // 1ï¿½ --> que otro socket ha lanzado el TPDU GRUPO_LOCAL_VECINO
            //        ante la recepciï¿½n de un mensaje BUSCAR_GRUPO_LOCAL_VECINO

            this.socketClusterNetImp.getTemporizador().cancelarFuncion(this.timerHandler,this.EVENTO_NOTIFICAR_GL_VECINOS);
            this.bNotificarGL= false;

            //Eliminar el ClusterGroupID....
            this.treeMapRespuestaGLVecinos.remove(tpduCGL.getIDGL());
        }
        else
        {
           //1. Cancelar temporizador de bï¿½squeda de grupo local
           //2.  y el flag correspondiente de notificaciï¿½n,
           //this.socketPTMFImp.getTemporizador().cancelarFuncion(this.timerHandler,EVENTO_BUSCAR_GL_VECINOS);

           //Eliminar el ClusterGroupID.... (AQUï¿½ por si acaso)
           //this.treeMapRespuestaGLVecinos.remove(tpduCGL.getIDGL());

           //Almacenar el IDGL_EMISOR como alcanzable por el ClusterGroupID Fuente...
           addIDGL(tpduCGL.getIDGL_EMISOR());

           //Almacenar toda la lista de IDGLs...
           addIDGL();
        }
     }
  }

  //==========================================================================
  /**
   * Envï¿½a una TPDUCGL BUSCAR_GRUPO_LOCAL_VECINO
   */
  private void enviarTPDU_CGL_BUSCAR_GRUPO_LOCAL_VECINO()
  {
    Buffer   buf       = null;
    String mn = "CLThread.enviarTPDU_CGL_BUSCAR_GRUPO_LOCAL_VECINO";


    //
    // TPDU --> BUSCAR_GRUPO_LOCAL_VECINO
    //

    try
    {

      tpduCGL =  TPDUCGL.crearTPDUCGL
      (/*SocketClusterNetImp*/      socketClusterNetImp ,
       /*byte SUBTIPO*/     (byte)ClusterNet.TPDU_CGL_BUSCAR_GRUPO_LOCAL_VECINO ,
       /*byte TTL */        (byte)this.TTL_BUSCAR_GRUPO_LOCAL_VECINO ,
       /*short METRICA*/    (short)0 ,
       /*boolean flagIP*/   false ,
       /*N_MAX_SOCKETS*/    this.N_MAX_SOCKETS_GL,
       /*N_SOCKETS*/        this.N_SOCKETS,
       /*IDGL_EMISOR*/      null,
       /*treeMapIDGL */     (TreeMap)null,
       /*treeMapID_SOCKET */(TreeMap)null ,
       /*ID_SOCKET */       (ClusterMemberID)null
       );



      buf = tpduCGL.construirTPDU();

      //Enviar el buffer....
      send(buf,this.TTL_BUSCAR_GRUPO_LOCAL_VECINO/*TTL*/);

      Log.debug(Log.CGL,mn,"TPDUCGL --> BUSCAR_GRUPO_LOCAL_VECINO  TTL:"+tpduCGL.getTTL());
      //Log.log("CGLThread","TPDUCGL --> BUSCAR_GRUPO_LOCAL_VECINO  TTL:"+tpduCGL.getTTL());

      //
      // Incrementar TTL +2.
      // Mï¿½trica = 0 [Bï¿½squeda del grupo local en la subred --> TTL =1]      //
      //

      //this.TTL = (byte)this.socketPTMFImp.getTTLSesion();
      if(this.TTL_BUSCAR_GRUPO_LOCAL_VECINO<2)
        this.TTL_BUSCAR_GRUPO_LOCAL_VECINO ++;
      else
        this.TTL_BUSCAR_GRUPO_LOCAL_VECINO += 2;

      //if (this.TTL_BUSCAR_GRUPO_LOCAL_VECINO > this.socketPTMFImp.getTTLSesion())
      //{
      //  this.TTL_BUSCAR_GRUPO_LOCAL_VECINO = this.socketPTMFImp.getTTLSesion();
      //}

    }
    catch (ClusterNetInvalidParameterException e){this.error(e);}
    catch (ClusterNetExcepcion e){this.error(e);}
    catch (IOException e) {this.error(e);}

  }




 //==========================================================================
  /**
   * Envï¿½a una TPDUCGL UNIRSE_A_GRUPO_LOCAL
   */
  private void enviarTPDU_CGL_UNIRSE_A_GRUPO_LOCAL()
  {
    Buffer   buf       = null;
    String mn = "CLThread.enviarTPDU_CGL_UNIRSE_A_GRUPO_LOCAL";

    Log.debug(Log.CGL,mn,"TPDUCGL --> UNIRSE_A_GRUPO_LOCAL"+"\n"+tpduCGL.getIDGL());

    //
    // TPDU --> UNIRSE_A_GRUPO_LOCAL
    //
      try
      {
        tpduCGL =  TPDUCGL.crearTPDUCGL
        (/*SocketClusterNetImp*/      this.socketClusterNetImp ,
         /*byte SUBTIPO*/     (byte)ClusterNet.TPDU_CGL_UNIRSE_A_GRUPO_LOCAL,
         /*byte TTL */        (byte)1 ,
         /*short METRICA*/    (short)0 ,
         /*boolean flagIP*/   true ,
         /*N_MAX_SOCKETS*/    this.N_MAX_SOCKETS_GL,
         /*N_SOCKETS*/        1, // 1 ID_SOCKET, el de este socket que se une al grupo.
         /*treeMapIDGL */     (TreeMap)null,
         /*treeMapID_SOCKET */(TreeMap)null ,
         /*ID_SOCKET */
          // Poner ID_SOCKET de este socket
          this.socketClusterNetImp.getID_Socket()
         );


          buf = tpduCGL.construirTPDU();

          //Enviar el buffer....
          send(buf,1/*TTL*/);

        }
        catch (ClusterNetInvalidParameterException e){this.error(e);}
        catch (ClusterNetExcepcion e){this.error(e);}
        catch (IOException e) {this.error(e);  }

  }



 //==========================================================================
  /**
   * Envï¿½a una TPDUCGL BUSCAR_GL_PARA_EMISOR
   * @param idglEmisor ClusterGroupID del Emisor para el que buscamos CG "PADRE"
   */
  private void enviarTPDU_CGL_BUSCAR_GL_PARA_EMISOR(ClusterGroupID idglEmisor)
  {
    Buffer   buf       = null;
    String mn = "CLThread.enviarTPDU_CGL_BUSCAR_GL_PARA_EMISOR";

    Log.debug(Log.CGL,mn,"TPDUCGL --> BUSCAR_GL_PARA_EMISOR"+"\n"+tpduCGL.getIDGL());

    //
    // TPDU --> BUSCAR_GL_PARA_EMISOR
    //
      try
        {
            // Poner ClusterGroupID Emisor pasado por parï¿½metro...
            TreeMap treemap = new TreeMap();
            treemap.put(idglEmisor,null);

            tpduCGL =  TPDUCGL.crearTPDUCGL
            (/*SocketClusterNetImp*/      this.socketClusterNetImp ,
             /*byte SUBTIPO*/     (byte)ClusterNet.TPDU_CGL_BUSCAR_GL_PARA_EMISOR,
             /*byte TTL */
              //
               (byte)this.TTL_BUSCAR_GRUPO_LOCAL_VECINO ,
             /*short METRICA*/    (short)0 ,
             /*boolean flagIP*/   false ,
             /*N_MAX_SOCKETS*/    this.N_MAX_SOCKETS_GL,
             /*N_SOCKETS*/        this.N_SOCKETS,
             /*treeMapIDGL */     (TreeMap)treemap ,
             /*treeMapID_SOCKET */(TreeMap)null ,
             /*ID_SOCKET */       null
             );

            buf = tpduCGL.construirTPDU();

            //Enviar el buffer....
            send(buf,(byte)this.getTTLGLMaximo()/*TTL*/);

            if(this.TTL_BUSCAR_GRUPO_LOCAL_VECINO<2)
              this.TTL_BUSCAR_GRUPO_LOCAL_VECINO ++;
            else
              this.TTL_BUSCAR_GRUPO_LOCAL_VECINO += 2;

            if (this.TTL_BUSCAR_GRUPO_LOCAL_VECINO > this.socketClusterNetImp.getTTLSesion())
            {
                this.TTL_BUSCAR_GRUPO_LOCAL_VECINO = this.socketClusterNetImp.getTTLSesion();
            }
        }
        catch (ClusterNetInvalidParameterException e){this.error(e);}
        catch (ClusterNetExcepcion e){this.error(e);}
        catch (IOException e) {this.error(e);  }
  }


 //==========================================================================
  /**
   * Envï¿½a una TPDUCGL GL_PARA_EMISOR notificando el CG para el grupo del emisor
   * preguntado.
   * @param idglEmisor ClusterGroupID del emisor para el que se busca un CG "Padre".
   * @param treeMapIDGLs Controladores de grupo "Padre" utilizados cuando emite el ClusterGroupID emisor
   * @param TTL utilizado para enviar los datos.
   */
  private void enviarTPDU_CGL_GL_PARA_EMISOR(ClusterGroupID idglEmisor, TreeMap treeMapIDGLs, byte TTL)
  {
    Buffer buf = null;
    String mn = "CLThread.enviarTPDU_CGL_GL_PARA_EMISOR";

    Log.debug(Log.CGL,mn,"TPDUCGL --> GL_PARA_EMISOR"+"\n"+tpduCGL.getIDGL());
    //
    // TPDU -->GL_PARA_EMISOR
    //
      try
        {

          // Poner ClusterGroupID del Emisor 1ï¿½ y despuï¿½s el del CG PAdre.....
          TreeMap treemap  = new TreeMap();
          treemap.put(idglEmisor,null);
          treemap.putAll(treeMapIDGLs);

          TPDUCGL tpduCGL =  TPDUCGL.crearTPDUCGL
          (/*SocketClusterNetImp*/      this.socketClusterNetImp ,
           /*byte SUBTIPO*/     (byte)ClusterNet.TPDU_CGL_GL_PARA_EMISOR,
           /*byte TTL */        TTL,          // TTL pasado por parï¿½metros para concretizar la distancia..
           /*short METRICA*/    (short)0 ,
           /*boolean flagIP*/   false ,
           /*N_MAX_SOCKETS*/    this.N_MAX_SOCKETS_GL,
           /*N_SOCKETS*/        this.N_SOCKETS,
           /*treeMapIDGL */     (TreeMap)treemap,
           /*treeMapID_SOCKET */(TreeMap)null ,
           /*ID_SOCKET */       null
           );


          buf = tpduCGL.construirTPDU();

          // Enviar el buffer..
          send(buf,TTL);
        }
        catch (ClusterNetInvalidParameterException e){this.error(e);}
        catch (ClusterNetExcepcion e){this.error(e);}
        catch (IOException e) {this.error(e);  }
  }

  //==========================================================================
  /**
   * Envï¿½a una TPDUCGL SOCKET_ACEPTADO_EN_GRUPO_LOCAL
   */
  private void enviarTPDU_CGL_SOCKET_ACEPTADO_EN_GRUPO_LOCAL(ClusterMemberID id_socket)
  {
    Buffer   buf       = null;
    String mn = "CLThread.enviarTPDU_CGL_SOCKET_ACEPTADO_EN_GRUPO_LOCAL";

    Log.debug(Log.CGL,mn,"TPDUCGL --> SOCKET_ACEPTADO_EN_GRUPO_LOCAL"+"\n"+tpduCGL.getIDGL());
    //
    // TPDU --> SOCKET_ACEPTADO_EN_GRUPO_LOCAL
    //

      try
        {
          tpduCGL =  TPDUCGL.crearTPDUCGL
          (/*SocketClusterNetImp*/      this.socketClusterNetImp ,
           /*byte SUBTIPO*/     (byte)ClusterNet.TPDU_CGL_SOCKET_ACEPTADO_EN_GRUPO_LOCAL,
           /*byte TTL */        (byte)1,
           /*short METRICA*/    (short)0 ,
           /*boolean flagIP*/   true ,
           /*N_MAX_SOCKETS*/    this.N_MAX_SOCKETS_GL,
           /*N_SOCKETS*/        this.N_SOCKETS,
           /*treeMapIDGL */     (TreeMap)null,
           /*treeMapID_SOCKET */this.treeMapID_Socket ,
           /*ID_SOCKET */       id_socket
           );

          // Log.log("---> treemap ID_SOCKETs "+this.treeMapID_Socket,"");
           buf = tpduCGL.construirTPDU();

           //Enviar el buffer....
           send(buf,1/*TTL*/);
        }
        catch (ClusterNetInvalidParameterException e){this.error(e);}
        catch (ClusterNetExcepcion e){this.error(e);}
        catch (IOException e) {this.error(e);  }

  }

  //==========================================================================
  /**
   * Envï¿½a una TPDUCGL GRUPO_LOCAL
   * @param TTL TTL usado para enviar la notificaciï¿½n de grupo local.
   */
  private void enviarTPDU_CGL_GRUPO_LOCAL(short TTL)
  {
    Buffer   buf       = null;
    int N_Ids = 0;
    String mn = "CLThread.enviarTPDU_CGL_GRUPO_LOCAL";

    //
    // TPDU --> ClusterNet.TPDU_CGL_GRUPO_LOCAL
    //
    try
    {

     tpduCGL =  TPDUCGL.crearTPDUCGL
     (/*SocketClusterNetImp*/      this.socketClusterNetImp ,
      /*byte SUBTIPO*/     (byte)ClusterNet.TPDU_CGL_GRUPO_LOCAL,
      /*byte TTL */        (byte)TTL,
      /*short METRICA*/    (short)0 ,
      /*boolean flagIP*/   false ,
      /*N_MAX_SOCKETS*/    this.N_MAX_SOCKETS_GL,
      /*N_SOCKETS*/        this.N_SOCKETS,
      /*IDGL_EMISOR*/      (ClusterGroupID)null,
      /*treeMapIDGL */     (TreeMap)null, /*MODIFICADO */
      /*treeMapID_SOCKET */(TreeMap)null,
      /*ID_SOCKET */       (ClusterMemberID)null
      );

      buf = tpduCGL.construirTPDU();

      //Enviar el buffer....
      send(buf,TTL/*TTL*/);

      Log.debug(Log.CGL,mn,"TPDUCGL --> GRUPO_LOCAL "+tpduCGL.getIDGL()+"TTL SEND: "+TTL);

      //  Log.log("CGLThread","TPDUCGL --> GRUPO_LOCAL "+tpduCGL.getIDGL()+" TTL SEND: "+TTL);
      if(treeMapIDGLs != null && treeMapIDGLs.size() > 0)
      {
        Iterator iterator = treeMapIDGLs.keySet().iterator();

        while(iterator.hasNext())
        {
          ClusterGroupID clusterGroupID = (ClusterGroupID) iterator.next();
          Log.debug(Log.CGL,mn,"ClusterGroupID --> "+clusterGroupID);
        }
      }
    }
    catch (ClusterNetInvalidParameterException e){this.error(e);}
    catch (ClusterNetExcepcion e){this.error(e);}
    catch (IOException e) {this.error(e);}

  }

  //==========================================================================
  /**
   * Envï¿½a un TPDU CGL GRUPO_LOCAL_VECINO
   * @param TTL TTL usado para enviar la notificaciï¿½n de grupo local.
   */
  private void enviarTPDU_CGL_GRUPO_LOCAL_VECINO(short TTL, ClusterGroupID idgl_Emisor)
  {
    Buffer   buf       = null;
    String mn = "CLThread.enviarTPDU_CGL_GRUPO_LOCAL_VECINO";

    TreeMap treemapIDGL = (TreeMap) this.treeMapIDGLs.clone(); //Clonar
    //Quitarnos de la lista
    treemapIDGL.remove(this.clusterGroupID);

    //
    // TPDU --> ClusterNet.TPDU_CGL_GRUPO_LOCAL_VECINO
    //
    try
    {

     tpduCGL =  TPDUCGL.crearTPDUCGL
     (/*SocketClusterNetImp*/    this.socketClusterNetImp ,
      /*byte SUBTIPO*/     (byte)ClusterNet.TPDU_CGL_GRUPO_LOCAL_VECINO,
      /*byte TTL */        (byte)TTL,
      /*short METRICA*/    (short)0 ,
      /*boolean flagIP*/   false ,
      /*N_MAX_SOCKETS*/    this.N_MAX_SOCKETS_GL,
      /*N_SOCKETS*/        this.N_SOCKETS,
      /*IDGL_EMISOR*/      idgl_Emisor,
      /*treeMapIDGL */     (TreeMap)treemapIDGL,
      /*treeMapID_SOCKET */(TreeMap)null,
      /*ID_SOCKET */       (ClusterMemberID)null
      );

      buf = tpduCGL.construirTPDU();

      //Enviar el buffer....
      send(buf,TTL/*TTL*/);

      //Log.debug(Log.CGL,"CGLThread","TPDUCGL --> GRUPO_LOCAL "+tpduCGL.getIDGL()+"TTL SEND: "+TTL);
      Log.debug(Log.CGL,mn,"TPDUCGL --> GRUPO_LOCAL_VECINO "+tpduCGL.getIDGL()+" TTL SEND: "+TTL);

      if(idgl_Emisor!=null)
        Log.log(" ","IDGL_EMISOR --> "+idgl_Emisor);

      if(treemapIDGL != null && treemapIDGL.size() > 0)
      {
        Iterator iterator = treemapIDGL.keySet().iterator();
        while(iterator.hasNext())
        {
          ClusterGroupID clusterGroupID = (ClusterGroupID) iterator.next();
          Log.debug(Log.CGL,mn,"ClusterGroupID --> "+clusterGroupID);
        }
      }
    }
    catch (ClusterNetInvalidParameterException e){this.error(e);}
    catch (ClusterNetExcepcion e){this.error(e);}
    catch (IOException e) {this.error(e);}

  }

  //==========================================================================
  /**
   * Envï¿½a una TPDUCGL DEJAR_GRUPO_LOCAL
   */
  private void enviarTPDU_CGL_DEJAR_GRUPO_LOCAL(ClusterMemberID id_socket)
  {
    Buffer   buf       = null;
    TreeMap  treeMapIPSocket = null;
    String mn = "CLThread.enviarTPDU_CGL_DEJAR_GRUPO_LOCAL";


    Log.debug(Log.CGL,mn,"TPDUCGL --> DEJAR_GRUPO_LOCAL"+"\n"+tpduCGL.getIDGL());
    //
    // TPDU --> ClusterNet.TPDU_CGL_GRUPO_LOCAL
    //
    try
    {

     tpduCGL =  TPDUCGL.crearTPDUCGL
     (/*SocketClusterNetImp*/      this.socketClusterNetImp ,
      /*byte SUBTIPO*/     (byte)ClusterNet.TPDU_CGL_DEJAR_GRUPO_LOCAL,
      /*byte TTL */        (byte)1,
      /*short METRICA*/    (short)0 ,
      /*boolean flagIP*/   true ,
      /*N_MAX_SOCKETS*/    this.N_MAX_SOCKETS_GL,
      /*N_SOCKETS*/        this.N_SOCKETS,
      /*treeMapIDGL */     (TreeMap)null,       /* ALEX ; FALTA MIRA DOCUMENTACION*/
      /*treeMapID_SOCKET */(TreeMap)null,       /* ALEX ; FALTA MIRA DOCUMENTACION*/
      /*ID_SOCKET */       id_socket
      );


      buf = tpduCGL.construirTPDU();

      //Enviar el buffer....
      send(buf,1/*TTL*/);

    }
    catch (ClusterNetInvalidParameterException e){this.error(e);}
    catch (ClusterNetExcepcion e){this.error(e);}
    catch (IOException e) {this.error(e);}

  }

  //==========================================================================
  /**
   * Envï¿½a una TPDUCGL ELIMINACION_GRUPO_LOCAL
   */
  private void enviarTPDU_CGL_ELIMINACION_GRUPO_LOCAL()
  {
    Buffer   buf       = null;
    String mn = "CLThread.ELIMINACION_GRUPO_LOCAL";

    Log.debug(Log.CGL,mn,"TPDUCGL --> ELIMINACION_GRUPO_LOCAL"+"\n"+tpduCGL.getIDGL());

    this.socketClusterNetImp.sendPTMFEventConexion("CGL: Eliminar Grupo Local: "+ this.clusterGroupID);

    //
    // TPDU --> ClusterNet.TPDU_CGL_ELIMINACION_GRUPO_LOCAL
    //
    try
    {

     tpduCGL =  TPDUCGL.crearTPDUCGL
     (/*SocketClusterNetImp*/      this.socketClusterNetImp ,
      /*byte SUBTIPO*/     (byte)ClusterNet.TPDU_CGL_ELIMINACION_GRUPO_LOCAL,
      /*byte TTL */        (byte)this.socketClusterNetImp.getTTLSesion(),
      /*short METRICA*/    (short)0 ,
      /*boolean flagIP*/   false,
      /*N_MAX_SOCKETS*/    this.N_MAX_SOCKETS_GL,
      /*N_SOCKETS*/        this.N_SOCKETS,
      /*treeMapIDGL */     (TreeMap)null,
      /*treeMapID_SOCKET */(TreeMap)null,
      /*ID_SOCKET */       (ClusterMemberID)null
      );


      buf = tpduCGL.construirTPDU();

      //Enviar el buffer....
      send(buf,(byte)this.socketClusterNetImp.getTTLSesion()/*TTL*/);

    }
    catch (ClusterNetInvalidParameterException e){this.error(e);}
    catch (ClusterNetExcepcion e){this.error(e);}
    catch (IOException e) {this.error(e);}

  }


  //==========================================================================
  /**
   * Envï¿½a una TPDUCGL BUSCAR_GRUPO_LOCAL.
   */
  private void enviarTPDU_CGL_BUSCAR_GRUPO_LOCAL()
  {
    Buffer   buf       = null;
    TreeMap  treeMapIPSocket = null;
    String mn = "CLThread.enviarTPDU_CGL_BUSCAR_GRUPO_LOCAL";

    Log.debug(Log.CGL,mn,"TPDUCGL --> BUSCAR_GRUPO_LOCAL");
    //Log.log("CGLThread","TPDUCGL --> BUSCAR_GRUPO_LOCAL");
    //
    // TPDU --> BUSCAR_GRUPO_LOCAL
    //
    try
    {
     tpduCGL =  TPDUCGL.crearTPDUCGL
     (/*SocketClusterNetImp*/      this.socketClusterNetImp ,
      /*byte SUBTIPO*/     (byte)ClusterNet.TPDU_CGL_BUSCAR_GRUPO_LOCAL,
      /*byte TTL */        (byte)1,
      /*short METRICA*/    (short)0 ,
      /*boolean flagIP*/   false,
      /*N_MAX_SOCKETS*/    this.N_MAX_SOCKETS_GL,
      /*N_SOCKETS*/        this.N_SOCKETS,
      /*treeMapIDGL */     (TreeMap)null,
      /*treeMapID_SOCKET */(TreeMap)null,
      /*ID_SOCKET */       (ClusterMemberID)null
      );


      buf = tpduCGL.construirTPDU();

      //Enviar el buffer....
      send(buf,1/*TTL*/);
    }
    catch (ClusterNetInvalidParameterException e){this.error(e);}
    catch (ClusterNetExcepcion e){this.error(e);}
    catch (IOException e) {this.error(e);}

  }

  //==========================================================================
  /**
   * ERROR. ESTE Mï¿½TODO SE LLAMA SI OCURRE UN ERROR GRAVE EN LA CLASE (Socket
   * Cerrado, etc.). Llama al mï¿½todo error de ClusterNet.
   * @see ClusterNet.
   */
  private void error(IOException e)
  {
     this.socketClusterNetImp.error(e);
  }



  //==========================================================================
  /**
   * Notificaciï¿½n de los temporizdores
   */
  private void notificacionesTemporizadores()
  {
    final String mn = "DataThread.notificacionesTemporizadores ()";

    //
    // Aceptaciï¿½n en grupo local del socket...
    //
    if(bAceptarSocket)
    {
     this.bAceptarSocket = false;

     while(this.colaAceptacionID_SOCKET.size() != 0)
     {
       ClusterMemberID id_socket = (ClusterMemberID) this.colaAceptacionID_SOCKET.removeFirst();

       //
       // Ver si no se ha pasado el lï¿½mite....
       //
       if(this.N_SOCKETS < this.N_MAX_SOCKETS_GL)
       {
         this.N_SOCKETS++;
         this.addID_Socket(id_socket);
         this.addID_Socket(this.socketClusterNetImp.getID_Socket());
         this.enviarTPDU_CGL_SOCKET_ACEPTADO_EN_GRUPO_LOCAL(id_socket);
       }
     }
    }

    //
    // Notificaciï¿½n del grupo local ...
    //
    if(this.bNotificarGL )
    {
      this.bNotificarGL = false;
      this.bLanzadoTemporizadorNotificacionGL = false;
      this.enviarTPDU_CGL_GRUPO_LOCAL(this.TTL_Notificacion_GL);
    }

    //
    // Notificaciï¿½n del grupo local a los GL vecinos...
    //
    if(this.bNotificarGLVecinos )
    {
      this.bNotificarGLVecinos = false;
      this.bLanzadoTemporizadorNotificacionGLVecino = false;

      //Enviar un TPDU para cada ClusterGroupID recibido pero
      // teniendo en cuenta que no se repitan los TPDU de forma innecesaria.

      Iterator iterator = this.treeMapRespuestaGLVecinos.keySet().iterator();
      short TTLMayor = 0;
      while(iterator.hasNext())
      {
        ClusterGroupID clusterGroupID = (ClusterGroupID) iterator.next();

        if (TTLMayor < clusterGroupID.TTL)
        {
          TTLMayor = clusterGroupID.TTL;
          //Enviar
          this.enviarTPDU_CGL_GRUPO_LOCAL_VECINO(clusterGroupID.TTL,clusterGroupID);

          //Eliminar el ClusterGroupID del treemap....
          iterator.remove();
        }
      }
    }

    //
    // Notificaciï¿½n de BUSQUEDA de EMISOR....
    //
    if (this.bNotificarBusquedaEmisor)
    {
      this.bNotificarBusquedaEmisor = false;
      Log.debug (Log.CGL,mn,"treemapbusquedaemisores: " + this.treeMapBusquedaEmisores);

      Iterator iterator = this.treeMapBusquedaEmisores.values().iterator();
      while(iterator.hasNext())
      {
        Intentos_Emisor intentos_emisor = (Intentos_Emisor)iterator.next();

        if(this.TTL_BUSCAR_GRUPO_LOCAL_VECINO >= this.socketClusterNetImp.getTTLSesion())
        {
          this.TTL_BUSCAR_GRUPO_LOCAL_VECINO = 1;
          Log.debug (Log.CGL,mn,"Intentos MAXIMOS de busqueda de GL PADRES para el ClusterGroupID Emisor "+intentos_emisor.idglEmisor+"EXCEDIDO!!!");

          //Despertar DataThread...
          //Log.log (mn,"POR LO MENOS ME HAN DESPERTADO.");
          this.semaforoDatosThread.up();

          /*if(intentos_emisor.numero_intentos > CGLThread.MAX_INTENTOS_BUSQUEDA_GL_EMISOR)
          {
            iterator.remove();
            Log.log("CGLThread.notificacionesTemporizadores","Intentos MAXIMOS de busqueda de GL PADRES para el ClusterGroupID Emisor "+intentos_emisor.idglEmisor+"EXCEDIDO!!!");

            //Despertar DataThread...
            Log.log (mn,"POR LO MENOS ME HAN DESPERTADO.");
            this.semaforoDatosThread.up();
          }
          else
          {
            enviarTPDU_CGL_BUSCAR_GL_PARA_EMISOR(intentos_emisor.idglEmisor);
            intentos_emisor.numero_intentos++;

            // Aï¿½ADIDO ESTA Lï¿½NEA. Sï¿½LO PARA PODER SEGUIR PROBANDO
            //ClusterTimer de espera para volver a reintentar...
            //
            this.socketPTMFImp.getTemporizador().registrarFuncion(this.T_ESPERA_BUSQUEDA_GL_EMISOR,
              timerHandler,this.EVENTO_BUSCAR_GL_EMISOR);
          }
          */
        }
        else
        {
            enviarTPDU_CGL_BUSCAR_GL_PARA_EMISOR(intentos_emisor.idglEmisor);
            intentos_emisor.numero_intentos++;

            // ANTONIO: Aï¿½ADIDO ESTA Lï¿½NEA. Sï¿½LO PARA PODER SEGUIR PROBANDO
            //ClusterTimer de espera para volver a reintentar...
            //
            this.socketClusterNetImp.getTemporizador().registrarFuncion(this.T_ESPERA_BUSQUEDA_GL_EMISOR,
             timerHandler,this.EVENTO_BUSCAR_GL_EMISOR);
        }
      }

      //Si quedan datos en la cola, lanzar el temporizador de nuevo
      if(treeMapBusquedaEmisores.size() > 0)
       //
       //ClusterTimer de espera ...
       //
      this.socketClusterNetImp.getTemporizador().registrarFuncion(T_ESPERA_BUSQUEDA_GL_EMISOR,timerHandler,EVENTO_BUSCAR_GL_EMISOR);
    }

    //
    // Notificar CG para EMISOR
    //
    if (bNotificarCGEmisor)
    {
      this.bNotificarCGEmisor = false;

      Iterator iterator = this.treeMapRespuestaBusquedaEmisores.keySet().iterator();

      while(iterator.hasNext())
      {
        ClusterGroupID clusterGroupID = (ClusterGroupID)iterator.next();

        //Enviar TTL...
        this.enviarTPDU_CGL_GL_PARA_EMISOR(clusterGroupID,this.getCGHijos(),(byte)clusterGroupID.TTL);
      }
    }

    //
    // Retransmitir UNIRSE_A_GRUPO_LOCAL
    //
    if (bRetransmitirUnirseGrupoLocal)
    {
       bRetransmitirUnirseGrupoLocal = false;

       //
       // TPDU --> UNIRSE_A_GRUPO_LOCAL
       //
       enviarTPDU_CGL_UNIRSE_A_GRUPO_LOCAL();

       //
       // Lanzar temporizador time out.....
       this.socketClusterNetImp.getTemporizador().registrarFuncion(T_ESPERA_ACEPTACION_EN_GRUPO_LOCAL,timerHandler,EVENTO_TIME_OUT);

    }
  }







  //==========================================================================
  /**
   * Comprobar la Cache
   */
  private void comprobarCache(ClusterGroupID clusterGroupID)
  {
     // NOTA: Comprobar si el clusterGroupID estï¿½ en las caches. Si es asï¿½ eliminamos
     // la entrada de la cache.
     //
     if( this.cachePadres.containsKey(clusterGroupID))
        this.cachePadres.remove(clusterGroupID);
     if( this.cacheHijos.containsKey(clusterGroupID))
        this.cacheHijos.remove(clusterGroupID);

  }


  //==========================================================================
  /**
   * Almacena un ClusterGroupID en la lista y actualiza la lista de IDGLs a los que alcanza<br>
   * @param clusterGroupID El ClusterGroupID key
   * @param treemap_idgl Un treemap de ClusterGroupID que alcanza el ClusterGroupID key.
   */
  private void addIDGL(ClusterGroupID idgl_key, TreeMap treemap_idgl)
  {
       boolean bNotificar = false;

       //Log.log("CGLThread","Almacenar ClusterGroupID -- "+idgl_key);

       if(idgl_key == null)
        return;

       //
       // Almacenar este ClusterGroupID en la lista de los ClusterGroupID a los que nosotros alcanzamos
       //

       if (!this.treeMapIDGLs.containsKey(idgl_key))
       {
           //Log.log("IDGL_KEY NO ESTA en la lista","");
           bNotificar = true;
       }
       else
       {
             //      Log.log("IDGL_KEY YA ESTA en la lista","");

       }

       if(treemap_idgl != null)
       {
           if(!this.treeMapIDGLs.containsKey(idgl_key))
           {
                   //Aï¿½adir el ClusterGroupID
                  this.treeMapIDGLs.put(idgl_key,new RegistroIDGL_TreeMap(idgl_key,treemap_idgl));
                  //Log.log("ClusterGroupID Aï¿½adido <--" +idgl_key,"");
           }
           else
           {
              RegistroIDGL_TreeMap reg = (RegistroIDGL_TreeMap) this.treeMapIDGLs.get(idgl_key);

              if(reg==null)
              {
                  this.treeMapIDGLs.put(idgl_key ,new RegistroIDGL_TreeMap(idgl_key,treemap_idgl));
              }

              if(reg != null)
              {
                  reg.treemap.putAll(treemap_idgl);

              }
           }
       }
       else
       {
            //Aï¿½adir el ClusterGroupID
            this.treeMapIDGLs.put(idgl_key,new RegistroIDGL_TreeMap(idgl_key,new TreeMap()));
            //Log.log("ClusterGroupID Aï¿½adido <--" +idgl_key,"");
       }


       //Comprobar cache
       comprobarCache(idgl_key);

       if(bNotificar)
           // Notificar Nuevo ClusterGroupID a los IDGLListeners registrados
           this.notificarNuevoIDGL(idgl_key);


       //Log.debug(Log.CGL,"CGLThread","Nï¿½ Hijos : "+(idgl_treemap.treemap.size()));
  }


  //==========================================================================
  /**
   * Almacena un ClusterGroupID en la lista y actualiza la lista de IDGLs a los que alcanza<br>
   */
  private void addIDGL()
  {
       //Log.log("CGLThread","Almacenar ClusterGroupID -- "+tpduCGL.getIDGL());

       this.addIDGL(tpduCGL.getIDGL(),tpduCGL.getTreeMapIDGL());
  }

  //==========================================================================
  /**
   * Almacena un ClusterGroupID  en la sublista de otro ClusterGroupID.<br>
   * @param idgl_GL_VECINO El 1ï¿½ ClusterGroupID del TPDU GRUPO_LOCAL_VECINO
   */
  private void addIDGL(ClusterGroupID idgl_GL_VECINO)
  {
       //Log.log("CGLThread","Almacenar ClusterGroupID  -- "+tpduCGL.getIDGL()+" como alcanzable por el ClusterGroupID: "+idgl_GL_VECINO);

       //
       // almacenarlo-...
       //

       TreeMap treemap = new TreeMap();

       if(idgl_GL_VECINO != null)
        treemap.put(idgl_GL_VECINO,null);


       this.addIDGL(tpduCGL.getIDGL(),treemap );

  }

  //==========================================================================
  /**
   * Almacena un ClusterMemberID y lo notifica a todos los ClusterMemberListener
   * si no estaba ya almacenados.
   */
  private void addID_Socket(ClusterMemberID id_socket)
  {

    if(!this.treeMapID_Socket.containsKey(id_socket))
    {
      //Almacenarlo....
      this.treeMapID_Socket.put(id_socket,null);

      //Notificarlo...
      this.notificarNuevoID_Socket(id_socket);
    }


    //Log.log("addID_socket() ---> tREEMAP id_sOCKETS "+this.treeMapID_Socket ,"");

  }


  //==========================================================================
  /**
   * Almacena una lista de ID_Sockets y notifica aquellos ClusterMemberID
   * que no estaban ya almacenados.
   */
  private void addID_Socket(TreeMap treeMap)
  {
    Iterator iterator = treeMap.keySet().iterator();

    while(iterator.hasNext())
    {
       ClusterMemberID id = (ClusterMemberID) iterator.next();
       this.addID_Socket(id);
    }
  }

  //==========================================================================
  /**
   * Send envï¿½a un buffer por el canal multicast con el TTL que se especifique.
   * @param buf El Buffer a enviar
   * @param ttl El TTL utilizado para enviar el buffer.
   */
  private void send(Buffer buf,int ttl) throws IOException
  {
          //
          // ENVIAR.. (Mandar n mensajes separados cada n mseg.
          //
          for(int i = 0; i<REDUNDANCIA_CGL; i++)
          {
            this.socketClusterNetImp.getCanalMcast().send(buf,(byte)ttl);
            this.socketClusterNetImp.getTemporizador().sleep(TIEMPO_REDUNDANCIA);
          }
  }


  //==========================================================================
  /**
   * Clase Intentos_Emisor.<br>
   * Almacena el Emisor y los nï¿½mero de intentos por cada emisor.
   */
   class Intentos_Emisor
   {
      short numero_intentos = 0;
      ClusterGroupID idglEmisor = null;

      Intentos_Emisor(ClusterGroupID idglEmisor, short intentos)
      {
       this.numero_intentos = intentos;
       this.idglEmisor = idglEmisor;
      }
   }


}


