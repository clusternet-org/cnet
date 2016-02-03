//============================================================================
//
//	Copyright (c) 1999-2015 . All Rights Reserved.
//
//----------------------------------------------------------------------------
//
//	Fichero: TablaID_SocketsEmisores.java  1.0 26/09/99
//
//
//	Descripción: Clase TablaID_SocketsEmisores. Almacena la información
//                   sobre los distintos emisores fuentes que hay en el grupo
//                   multicast.
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

import java.util.TreeMap;
import java.util.Iterator;

/**
 * Clase que almacena información sobre ID_Sockets que son fuentes de datos en el
 * grupo multicast.
 * Esta clase no es thread-safe.
 * @version  1.0
 * @author Antonio Berrocal Piris
 * <A HREF="mailto:AntonioBP@wanadoo.es">(AntonioBP@wanadoo.es)</A><p>
 * M. Alejandro García Domínguez
 * <A HREF="mailto:alejandro.garcia.dominguez@gmail.com">(alejandro.garcia.dominguez@gmail.com)</A><p>
 */
public class TablaID_SocketsEmisores
{

 //ATRIBUTOS
 /**
  * Almacena la información sobre los id_sockets fuentes de datos.<br>
  * <table border=1>
  *  <tr>  <td><b>Key:</b></td>
  *	    <td>{@link ClusterMemberID} fuente.</td>
  *  </tr>
  *  <tr>  <td><b>Value:</b></td>
  *	    <td>Instancia de {@link RegistroTHE registro Tabla Emisores}.</td>
  *  </tr>
  * </table>
  */
 private TreeMap treeMapTablaID_Sockets = null;



 //==========================================================================
 /**
  * Constructor.
  */
  public TablaID_SocketsEmisores()
  {
   this.treeMapTablaID_Sockets = new TreeMap ();
  }

  //==========================================================================
  /**
   * Añade un nuevo id_socket al que se le asocia una ventana de recepción.
   * @param id_Socket emisor fuente de datos a añadir
   * @param clusterGroupID grupo local al que pertenece el id_Socket
   * @param iTtamMaxVtaRecep tamaño máximo de la ventana de recepción asociada
   * al id_Socket.
   * @param nSecInic número de secuencia inicial de la ventana de recepción
   * asociada al id_Socket.
   * @return true si se ha añadido, y false en caso contrario.
   * @exception ClaveDuplicadaExcepcion lanzada si id_Socket ya estaba registrado
   * @exception ClusterNetInvalidParameterException lanzada si nSecInic no es válido.
   */
  public void addID_Socket (ClusterMemberID id_Socket,
                            ClusterGroupID clusterGroupID,
                            int iTamMaxVtaRecep,
                            SecuenceNumber nSecInic)
                                throws ClaveDuplicadaExcepcion,ClusterNetInvalidParameterException
  {
   if (this.treeMapTablaID_Sockets.containsKey (id_Socket))
      throw new ClaveDuplicadaExcepcion (
                    "Ya existe un ClusterMemberID emisor con la dirección indicada" + id_Socket);

   // Crear el registro para este ID_Sockets.
   RegistroTHE reg = new RegistroTHE ();

   reg.rxWindow = new RxWindow (iTamMaxVtaRecep,nSecInic.tolong ());
   // No se ha entregado ningún TPDU al usuario procedente de id_Socket
   // ClusterGroupID del emisor
   reg.clusterGroupID = clusterGroupID;

   // Actualizamos el tiempo.
   reg.lTiempoUltimaRecepcion = ClusterTimer.tiempoActualEnMseg();

   this.treeMapTablaID_Sockets.put (id_Socket,reg);
  }

  //===========================================================================
  /**
   * Elimina id_Socket.
   * @param id_Socket
   * @return true si existía un ClusterMemberID y ha sido eliminado,o false en
   * caso contrario.
   */
  public boolean removeID_Socket (ClusterMemberID id_Socket)
  {
   if (this.treeMapTablaID_Sockets.remove (id_Socket)==null)
        return false;
   return true;
  }

  //==========================================================================
  /**
   * Actualiza el instante de tiempo último en que se ha recibido un {@link TPDUDatosNormal}
   * del id_Socket fuente indicado.
   * @param id_socket
   * @return true si id_socket está registrado y ha sido actualizado.
   */
  public boolean actualizarTiempoUltimaRecepcion (ClusterMemberID id_socket)
  {
   RegistroTHE registroTHE = (RegistroTHE)this.treeMapTablaID_Sockets.get (id_socket);
   if (registroTHE==null)
     return false;

   long lTActual = ClusterTimer.tiempoActualEnMseg();
   if (lTActual > registroTHE.lTiempoUltimaRecepcion)
       registroTHE.lTiempoUltimaRecepcion = lTActual;
   return true;
  }

  //==========================================================================
  /**
   * Comprueba si ha transcurrido el tiempo máximo de inactividad {@link ClusterNet#TIEMPO_MAX_INACTIVIDAD_EMISOR}
   * permitido para los id_sockets fuentes de datos.
   * @return id_sockets que han superado el tiempo de inactividad máximo sin
   * que hayan enviado un {@link TPDUDatosNormal} al grupo multicast.
   * <table border=1>
   *  <tr>  <td><b>Key:</b></td>
   *	    <td>{@link ClusterMemberID} fuente.</td>
   *  </tr>
   *  <tr>  <td><b>Value:</b></td>
   *	    <td>NULL.</td>
   *  </tr>
   * </table>
   */
   public TreeMap comprobarInactividad()
   {
    TreeMap treeMapResult = new TreeMap();

    // Recorrer la tabla de CG Locales y ver quien ha superado el tiempo máximo
    // de inactividad.
    Iterator iteradorID_Socket = this.treeMapTablaID_Sockets.keySet().iterator();

    long lTActual = ClusterTimer.tiempoActualEnMseg();
    RegistroTHE registroTHENext;
    ClusterMemberID id_socketNext;
    while (iteradorID_Socket.hasNext())
     {
       id_socketNext = (ClusterMemberID)iteradorID_Socket.next();
       registroTHENext = (RegistroTHE)this.treeMapTablaID_Sockets.get (id_socketNext);

       if (registroTHENext==null)
        continue;

       if ( (lTActual - registroTHENext.lTiempoUltimaRecepcion)
             >
            ClusterNet.TIEMPO_MAX_INACTIVIDAD_EMISOR )
           treeMapResult.put (id_socketNext,null);
     }
    return treeMapResult;
   }

  //===========================================================================
  /**
   * Entrega al usuario de ClusterNet los datos que se hayan recibido correctamente
   * y que sean consecutivos del último entregado.
   * @param id_socket identificador del socket fuente de los datos a entregar.
   * @param rxQueue cola donde se depositarán los datos.
   * @return true si no hay nada disponible que entregar al usuario o se ha
   * entregado todo lo disponible, y false en caso contrario.
   */
  public boolean entregaDatosUsuario (ClusterMemberID id_socket,RxQueue rxQueue)
  {
   String mn ="TablaID_SocketEmisores.entregaDatosUsuario";

   if ((id_socket==null) || (rxQueue == null))
        return false;

   RegistroTHE registroTHE = (RegistroTHE) this.treeMapTablaID_Sockets.get (id_socket);

   if (registroTHE == null)
        return false;

   long nSecConsecutivo = registroTHE.rxWindow.getNumSecConsecutivo ();

   if (nSecConsecutivo<0)
        return false;

   // ¿Qué pasa con los métodos de la ventana que modifican los números de
   // secuencia de la ventana ?
   if (registroTHE.lNSecAEntregar<registroTHE.rxWindow.getNumSecInicial())
   {
      // Entregar el inicial de la ventana.
       registroTHE.lNSecAEntregar = registroTHE.rxWindow.getNumSecInicial();
   }
   boolean bResult = false;
   for (;registroTHE.lNSecAEntregar<=nSecConsecutivo;registroTHE.lNSecAEntregar++)
     {
      TPDUDatosNormal tpduDatosNormal = registroTHE.rxWindow.
                                 getTPDUDatosNormal (registroTHE.lNSecAEntregar);

      if (tpduDatosNormal.getBufferDatos()!=null)
       {
        if (!rxQueue.add(id_socket,tpduDatosNormal.getBufferDatos(),tpduDatosNormal.getFIN_TRANSMISION()))
          {
            Log.debug (Log.DATOS_THREAD,mn,"COLA DE RECEPCION LLENA");
            Log.debug (Log.DATOS_THREAD,mn,"Tamaño de la cola de recepcion: " + rxQueue.getTamaño());
            Log.debug (Log.DATOS_THREAD,mn,"Bytes en la cola de recepcion: " + rxQueue.getCapacidad());
            Log.debug (Log.DATOS_THREAD,mn,"Tamaño del buffer a añadir: " + tpduDatosNormal.getBufferDatos().getMaxLength());
            bResult=true;
            break;
            //return false;//CONTINUAR AQUI --> ALEX: 2/04/2003
                           // Será por que la cola esta llena, o algo así.
                           // Puede ser un buen punto para averiguar problemas
                           // de control del flujo.
                           // Devolvemos false por que aunque le hayamos entregado
                           // algo, no le hemos podido entregar todo lo disponible.
          }
        }
      bResult = true;
     }
  return bResult;
  }

  //===========================================================================
  /**
   * Comprueba si id_tpdu ha sido entregado al usuario.<br>
   * <b>Nota:</b> Si este ha sido entregado, también se habrán entregado todos
   * los datos anteriores (se entrega en orden).
   * @param id_tpdu
   * @return true si ha sido entregado.
   */
  public boolean entregadoAlUsuario (ID_TPDU id_tpdu)
  {
   if (id_tpdu == null)
     return false;

   RegistroTHE registroTHE = (RegistroTHE) this.treeMapTablaID_Sockets.get (id_tpdu.getID_Socket());

   if (registroTHE == null)
        return false;


   if (registroTHE.lNSecAEntregar>id_tpdu.getNumeroSecuencia().tolong())
     {
       return true;
     }
   else // Comprobar si es menor del inicial de la ventana.
        {
         if (registroTHE.rxWindow.getNumSecInicial()
                                        > id_tpdu.getNumeroSecuencia().tolong())
               return true;
        }

   return false;
  }

  //===========================================================================
  /**
   * Devuelve una referencia a la ventana de recepción asociada a id_Socket.<br>
   * <b>Las modificaciones realizadas a la ventana de recepción, a través de la
   * referencia devuelta o de la almacenada en la tabla, se reflejará en los
   * dos extremos.</b>
   * @param id_Socket
   * @return ventana de recepción asociada a id_Socket, o null si no existe.
   */
  public RxWindow getVentanaRecepcion (ClusterMemberID id_Socket)
  {
   RegistroTHE reg = (RegistroTHE)this.treeMapTablaID_Sockets.get (id_Socket);

   if (reg==null)
        return null;
   return reg.rxWindow;
  }

  //==========================================================================
  /**
   * Devuelve el ClusterGroupID del ClusterMemberID
   * @param id_Socket
   * @return clusterGroupID identificador del grupo local al que pertenece id_Socket.
   */
  public ClusterGroupID getIDGLFuente (ClusterMemberID id_Socket)
  {
   RegistroTHE reg = (RegistroTHE)this.treeMapTablaID_Sockets.get (id_Socket);

   if (reg==null)
        return null;
   return reg.clusterGroupID;
  }

  //==========================================================================
  /**
   * Devuelve un treeMap con los ID_Sockets fuentes.
   * @return
   * <table border=1>
   *  <tr>  <td><b>Key:</b></td>
   *	    <td>{@link ClusterMemberID} fuente.</td>
   *  </tr>
   *  <tr>  <td><b>Value:</b></td>
   *	    <td>NULL.</td>
   *  </tr>
   * </table>
   */
  public TreeMap getID_SocketEmisores ()
  {
    Iterator iteradorID_Socket = this.treeMapTablaID_Sockets.keySet().iterator();

    TreeMap treeMapResult = new TreeMap();
    while (iteradorID_Socket.hasNext())
      treeMapResult.put (iteradorID_Socket.next(),null);

    return treeMapResult;
  }

  //==========================================================================
  /**
   * Devuelve una cadena informativa.
   */
  public String toString ()
  {
   return this.treeMapTablaID_Sockets.toString ();
  }

}


/**
 * Guarda la información de un ClusterMemberID que es fuente de datos.
 * @see TablaID_SocketsEmisores#treeMapTablaID_Sockets
 * @version  1.0
 * @author M. Alejandro García Domínguez
 * <A HREF="mailto:alejandro.garcia.dominguez@gmail.com">(alejandro.garcia.dominguez@gmail.com)</A><p>
 *			   Antonio Berrocal Piris
 */
class RegistroTHE
{
  /** Window de recepción asociada al ClusterMemberID asociado al registro*/
  RxWindow rxWindow = null;

  /** ClusterGroupID del ClusterMemberID */
  ClusterGroupID clusterGroupID = null;

  /**
    * Instante de tiempo (mseg) en que se ha recibido el último {@link TPDUDatosNormal}
    * del id_Socket asociado al registro.
    */
  long lTiempoUltimaRecepcion = 0;

  /** Número de secuencia que se tiene que entregar al usuario. Los anteriores
    * a este ya han sido entregados. */
  long lNSecAEntregar = -1;

  //==========================================================================
  /**
   * Devuelve una cadena inforamativa.
   */
  public String toString ()
  {
   return this.rxWindow.toString () +
          this.clusterGroupID.toString() +
          "Tiempo Ultima Recepcion: " + this.lTiempoUltimaRecepcion +
          "NSec siguiente a entregar usuario: " + this.lNSecAEntregar;
  }
}
