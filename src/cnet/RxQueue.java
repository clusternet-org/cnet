//============================================================
//
//	Copyright (c) 1999-2015 . All Rights Reserved.
//
//------------------------------------------------------------
//
//	Fichero: RxQueue.java  1.0 24/11/99
//
//
//	Descripción: Clase ColaRcepcion.
//               Clase RegistroEmisor_Buffer
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
//------------------------------------------------------------

package cnet;

import java.util.LinkedList;
import java.util.TreeMap;
import java.util.Iterator;
import java.io.IOException;

/**
 * RxQueue es un buffer de recepción para los datos que se obtienen
 * del canal multicast, para cada socket Id_Socket del que se obtenga
 * datos se crea un ClusterMemberInputStream donde se almacena los datos para
 * que el usuario pueda acceder a ellos mediante los métodos sobreescritos
 * de la clase InputStream.<br>
 * Esta clase es thread-safe.
 * @version  1.0
 * @author
 *  Antonio Berrocal Piris
 *  <A HREF="mailto:AntonioBP@wanadoo.es">(AntonioBP@wanadoo.es)</A><br>
 *  M. Alejandro García Domínguez
 *  <A HREF="mailto:alejandro.garcia.dominguez@gmail.com">(alejandro.garcia.dominguez@gmail.com)</A><p>
 *
 * @see Buffer
 */


class RxQueue implements TimerHandler
{

  /**
   * TreeMap key: ClusterMemberID value: ClusterMemberInputStream
   */
  private TreeMap treemap = null;

  /** socket */
  private SocketClusterNetImp socket = null;

  /** Lista de Objetos Id_Socket_Buffer */
  private LinkedList listaID_Socket_Buffer = null;

  /** La capacidad de la cola */
  private int iCapacidad = 0;

  /** El tamaño actual de la cola */
  private int iTamaño = 0;

  /** Semáforo */
  private Semaforo semaforo = null;

  /** Time-Out*/
  private int iTimeOut = 0;

  /**
   * Indica si los datos que vayan llegando a la cola de recepción serán
   * encolados o simplemente no se tendrán en cuenta.
   */
  private boolean bEncolarDatos = true;

  /** Mutex */
  private Mutex mutex = null;

  /**  Lista de objetos PTMFId_SocketInputStreamListeners  */
  private LinkedList  listaPTMFID_SocketInputStreamListeners = null;

  //==========================================================================
  /**
   * Contructor genérico.
   * @param iCapacidad Capacidad de la cola en bytes.
   * @param socket El objeto SocketClusterNetImp
   */
  RxQueue(int iCapacidad, SocketClusterNetImp socket)
  {
    this.iCapacidad = iCapacidad;
    this.socket = socket;

    if((socket.getModo() == ClusterNet.MODE_RELIABLE)||(socket.getModo () == ClusterNet.MODE_DELAYED_RELIABLE))
    {
      this.treemap = new TreeMap();
      this.listaPTMFID_SocketInputStreamListeners = new LinkedList();
     }
    else
      this.listaID_Socket_Buffer = new LinkedList();

    try
    {
      semaforo = new Semaforo(true,1);
      mutex = new Mutex();
    }
    catch(ClusterNetInvalidParameterException e)
    {
     Log.log("TXQueue --> ERROR FATAL",e.getMessage());
    }

  }

  //==========================================================================
  /**
   * Obtener la capacidad de la cola, expresado en bytes.
   * @return un entero con la capacidad en bytes de la cola
   */
  int getCapacidad(){return this.iCapacidad; }

  //==========================================================================
  /**
   * Obtener el tamaño de la cola.El número de bytes que hay en la cola.
   * @return un entero con el tamaño de la cola
   */
  int getTamaño(){ return this.iTamaño; }


  //==========================================================================
  /**
   * Establece el tamaño de la cola
   * @param n_bytes Número de bytes a decrementar el tamaño
   * @return El tamaño de la cola decrementado.
   */
  int decrementarTamaño(int n_bytes)
  {
    try
    {
      //Sincronizar threads...
      this.mutex.lock();


      this.iTamaño -= n_bytes;

      //Log.log("DECrementar tamaño de la RxQueue: -"+n_bytes+" TAMAÑO: "+tamaño,"");
      if(this.iTamaño <0)
        this.iTamaño = 0;

      return this.iTamaño;
    }

    finally
    {
      //Sincronizar threads...
      this.mutex.unlock();
    }
  }


  //==========================================================================
  /**
   * Establece el valor de la variable encolarDatos. LLama a la función reset()
   * @param bEncolarDatosParam true si se quiere encolar los datos, false en caso contrario.
   */
  void setEncolarDatos (boolean bEncolarDatosParam)
  {
   try
   {
      //Sincronizar threads...
      this.mutex.lock();

     if (bEncolarDatosParam)
     {
       this.bEncolarDatos = true;
       return;
     }

     // encolarDatosParam es false
     if (bEncolarDatosParam)
     {
        //Sincronizar threads...
       this.mutex.unlock();
       this.reset();
       //Sincronizar threads...
       this.mutex.lock();

       this.bEncolarDatos = false;
     }

     return;
   }

   finally
   {
      //Sincronizar threads...
      this.mutex.unlock();
   }

  }

  //==========================================================================
  /**
   * Añadir un objeto Buffer y Address del emisor del buffer al final
   * de la cola.<br>
   * <b> NI EL BUFFER NI EL ClusterMemberID SE COPIAN.</b>
   * @param emisor El objeto Address del emisor que envió el Buffer.
   * @param buf El Buffer
   * @param bFinTransmision Bit de Fin de Transmision.
   * @return true si la operación se ha realizado con éxito, false en caso contrario
   */
  boolean add(ClusterMemberID id_socket,Buffer buf,boolean bFinTransmision)
  {
    ClusterMemberInputStream in = null;

    try
    {
      //Sincronizar threads...
      this.mutex.lock();


      if (!bEncolarDatos)
        return true;

      if(buf == null)
        throw new NullPointerException("RxQueue.add: Parámetro buf nulo");

      int longitudBuf = buf.getLength();

      if(longitudBuf + this.iTamaño > this.iCapacidad)
        return false;

      if(id_socket == null)
        throw new NullPointerException("RxQueue.add: Parámetro id_socket nulo");


      //Modo Fiable
      if((this.socket.getModo() == ClusterNet.MODE_RELIABLE)||(this.socket.getModo()==ClusterNet.MODE_DELAYED_RELIABLE))
      {
        if(this.treemap.containsKey(id_socket))
          in = (ClusterMemberInputStream)this.treemap.get(id_socket);

        else
        {
          in = new ClusterMemberInputStream(id_socket,this.socket);
          this.treemap.put(id_socket,in);
          //Log.log("NUEVO ID_SOCKETIMPUTASTREAM para  el socket:" +id_socket,"");
          //Notificar Nuevo ClusterMemberInputStream
          this.sendPTMFEventId_SocketInputStream(in);
       }


        //COMPROBAR SI EL DATO RECIBIDO ES FIN DE FLUJO
        if(!bFinTransmision)
        {
          // Añadir el Buffer a la lista
          if (!in.add(new RegistroBuffer(buf,bFinTransmision)))
            return false;
          // calcular el nuevo tamaño....
          this.iTamaño += buf.getLength();
          // Log.log ("RxQueue","Añadido datos sin problemas.");
        }
        else      //Si Fin Emision --> ELIMINAR EL ID_SOCKETINPUTSTREAM DEL TREEMAP
        {
          //try
          //{
           //if ((buf.getLength() != 1) && (buf.getByte(0) != (short)0xFF))
           //{
              // Añadir el Buffer a la lista
              if (!in.add(new RegistroBuffer(buf,bFinTransmision)))
                return false;
              //Log.log ("RxQueue","Añadido datos sin problemas.");
              // calcular el nuevo tamaño....
              this.iTamaño += longitudBuf;

          // }
          //}
          //catch(ClusterNetInvalidParameterException e){;}

          //FIN DE FLUJO.
          this.treemap.remove(id_socket);
        }

      }
      else //Modo No-Fiable y no_fiable_ordenado
      {
        this.listaID_Socket_Buffer.add(new RegistroID_Socket_Buffer(id_socket,buf,bFinTransmision));

        // calcular el nuevo tamaño....
        this.iTamaño += longitudBuf;
      }


      //levantar el semáforo
      this.semaforo.up();

      //Sincronizar threads...
      this.mutex.unlock();

      //Notificar al usuario que se han recibido nuevos datos...
      // NOTA Esta notificacion es valida si el usuario se ha registrado
      // en las clases DatagramSocket o ClusterNetInputStream.
      this.socket.sendPTMFEventDatosRecibidos("Datos Recibidos",buf.getLength());

      //Sincronizar threads...
      this.mutex.lock();
      //Log.log("COLA RECEPCION "," ADD -> Tamaño: "+buf.getLength());
      //Log.log("COLA RECEPCION ",""+buf);

      return true;

    }
    finally
    {
      //Sincronizar threads...
      this.mutex.unlock();
    }
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
  public void addPTMFID_SocketInputStreamListener(ClusterNetMemberInputStreamListener obj)
  {
      //Log.log("\n\nLISTENER AÑADIDO","");
      this.listaPTMFID_SocketInputStreamListeners.add(obj);
  }

  //==========================================================================
  /**
   * Elimina un objeto ClusterNetRxDataListener
   */
  public void removePTMFID_SocketInputStreamListenerListener(ClusterNetMemberInputStreamListener obj)
  {
    this.listaPTMFID_SocketInputStreamListeners.remove(obj);
  }

  //==========================================================================
  /**
   * Eliminar un ClusterMemberInputStream asociado a un ClusterMemberID.
   * NOTA: ES LLAMADO POR ID_SCOKETINPUTSTREAM EN EL CLOSE()
   *        Y POR DATOSTHREAD
   * @param id_socket El objeto h aeliminar.
   */
  void remove(ClusterMemberID id_socket)
  {
    try
    {
      //Sincronizar threads...
      this.mutex.lock();

     //Modo Fiable
     if((this.socket.getModo() == ClusterNet.MODE_RELIABLE)||(this.socket.getModo()==ClusterNet.MODE_DELAYED_RELIABLE))
     {
      //Si hay datos en el flujo ClusterMemberInputStream asociado,
      //Añadir el ClusterMemberID a eliminar al treemap de eliminación ...
      ClusterMemberInputStream id = (ClusterMemberInputStream)this.treemap.get(id_socket);
      if(id!=null)
        //Cerrar el flujo...
        id.closeStream();

      //Eliminar el flujo...
      this.treemap.remove(id_socket);
      //Log.log("Eliminado el flujo para el socket:" +id_socket,"");

     }
     //NOTA: El tamaño se actualiza cuando se lee del flujo ClusterMemberInputStream.

     return;
    }
    finally
    {
      //Sincronizar threads...
      this.mutex.unlock();
    }

  }

  //==========================================================================
  /**
   * Elimina todos los recursos de la cola de Recepcion
   */
  void reset()
  {
    try
    {
      //Sincronizar threads...
      this.mutex.lock();

      this.iTamaño = 0;

      if((this.socket.getModo() == ClusterNet.MODE_RELIABLE)||(this.socket.getModo() ==ClusterNet.MODE_DELAYED_RELIABLE))
        // vaciar el treemap
        this.treemap.clear();
      else
        this.listaID_Socket_Buffer.clear();
    }
    finally
    {
      //Sincronizar threads...
      this.mutex.unlock();
    }
  }

  //==========================================================================
  /**
   * Envía un evento ClusterNetEvent del tipo EVENTO_DATOS_RECIBIDOS
   * con una cadena informativa.
   * @param mensaje Mensaje Informativo
   * @param id_socket Objeto ClusterMemberID que ha sido eliminado
   */
   private void sendPTMFEventId_SocketInputStream(ClusterMemberInputStream id)
   {
    if (id == null)
      return;

    if (this.listaPTMFID_SocketInputStreamListeners.size() != 0)
    {
     //Log.log("\n\nENVIANDO EVENTGO","");

     ClusterNetEventMemberInputStream evento = new ClusterNetEventMemberInputStream(this.socket,"Nuevo ClusterMemberInputStream",id);

     Iterator iterator = this.listaPTMFID_SocketInputStreamListeners.listIterator();
     while(iterator.hasNext())
     {
        ClusterNetMemberInputStreamListener ptmfListener = (ClusterNetMemberInputStreamListener)iterator.next();
        ptmfListener.actionMemberInputStream(evento);
     }
    }
   }

  //==========================================================================
  /**
   * Establece la capacidad de la cola en bytes
   * @param la nueva capacidad de la cola de emisión
   */
  void setCapacidad(int capacidad)
  {
    try
    {
      //Sincronizar threads...
      this.mutex.lock();

      if(iCapacidad > 0)
        this.iCapacidad = iCapacidad;


      return;
    }
    finally
    {
      //Sincronizar threads...
      this.mutex.unlock();

    }
  }

  //==========================================================================
  /**
   * Obtiene el treemap de la cola.
   * @return El treemap de la cola.
   */
  TreeMap getTreeMap()
  {
    try
    {
      //Sincronizar threads...
      this.mutex.lock();

      return this.treemap;
    }
    finally
    {
       //Sincronizar threads...
      this.mutex.unlock();
    }
  }

  //==========================================================================
  /**
   * Obtiene el siguiente dato recibido.<br>
   * ESTE MÉTODO SOLO ES VÁLIDO EN EL MODO NO-FIABLE.<BR>
   * Para averiguar si hay datos en la cola utilice la función getTamaño()<br>
   * @param id_socket ClusterMemberID del socket emisor de los datos
   * @param buf Buffer con los datos emitidos por el socket Id_Socket
   * @return true si se ha realizado la operación con éxito, falso
   *  en caso contrario.
   * @exception java.io.InterruptedIOException Se lanza si se alcanza el TimeOut.
   */
  RegistroID_Socket_Buffer remove() throws java.io.InterruptedIOException
  {
    RegistroID_Socket_Buffer id_socket_buffer = null;

    try
    {
     //Sincronizar threads...
     this.mutex.lock();


     for(;;)
     {
      if(this.socket.getModo()  == ClusterNet.MODE_NO_RELIABLE)
      {
       if((this.iTamaño > 0) && (!this.listaID_Socket_Buffer.isEmpty()))
       {
        id_socket_buffer= (RegistroID_Socket_Buffer) this.listaID_Socket_Buffer.removeFirst();

        //--> ACTUALIZAR TAMAÑO
        this.iTamaño -= id_socket_buffer.getBuffer().getLength();
       }
       else if(this.iTamaño <= 0)
       {
          this.socket.getTemporizador().cancelarFuncion(this,0);

          //Establecer SO_TIMEOUT
          if(this.iTimeOut > 0)
            this.socket.getTemporizador().registrarFuncion(iTimeOut,this,0);


          //Sincronizar threads...
          this.mutex.unlock();

          //
          // No hay datos que leer, bloquear al thread
          //
          this.semaforo.down();

          //Sincronizar threads...
          this.mutex.lock();

          if(this.iTamaño > 0)
           continue ;
          else   //Me ha despertado el TIME OUT....
            throw new java.io.InterruptedIOException("TIME OUT");
       }
      }

      return id_socket_buffer;

     }//fin-for
    }
    finally
    {
      //Sincronizar threads...
      this.mutex.unlock();

    }
  }


  /**
   * Obtiene el siguiente flujo ID_socketInputStream
   * @return ClusterMemberInputStream
   */
  ClusterMemberInputStream nextID_SocketInputStream()
  {
    Iterator iterator = null;
    try
    {
     //Sincronizar threads...
     this.mutex.lock();

     if(iterator == null)
       iterator = this.treemap.values().iterator();

      if(iterator == null) return null;

      while(iterator.hasNext())
      {
         ClusterMemberInputStream in = (ClusterMemberInputStream)iterator.next();

         if(in.available() > 0) //¿Tiene este buffer datos, palomo?
          return in;
         //else          //¿HAy que eliminar el Flujo? --> FIN
         //  iterator.remove();
     }

      return null; // No se ha encontrado un flujo con datos.
    }
    catch(IOException e)
    {
     return null;
    }

    finally
    {
      //Sincronizar threads...
      this.mutex.unlock();
    }
  }

  //==========================================================================
  /**
   * Establece el tiempo de espera máximo que el thread de usuario espera
   * en una llamada al método receive() sin que hallan llegado datos.
   * @param iTiempo Tiempo máximo de espera en mseg. 0 espera infinita.
   */
  void setTimeOut(int iTiempo)
  {
    this.iTimeOut = iTiempo;

    if(this.iTimeOut == 0)
      this.socket.getTemporizador().cancelarFuncion(this,0);
  }

  //==========================================================================
  /**
   * Devuelve el tiempo de espera máximo que el thread de usuario espera
   * en una llamada al método receive() sin que hallan llegado datos.
   * @return  iTiempo Tiempo máximo de espera en mseg. 0 espera infinita.
   */
  int getTimeOut()
  {
     return this.iTimeOut;
  }

  /** TimerCallback */
  public void TimerCallback(long larg1,Object obf1)
  {
    this.semaforo.up();
  }
}





