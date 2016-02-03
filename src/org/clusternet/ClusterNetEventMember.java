//============================================================
//
//	Copyright (c) 1999-2015 . All Rights Reserved.
//
//------------------------------------------------------------
//
//	Fichero: ClusterNetEventMember.java  1.0 14/03/2000
//
//	Descripción: Clase ClusterNetEventMember. Evento ClusterNet ID_Socket
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

package org.clusternet;


import java.util.EventObject;

/**
 * La clase ClusterNetEventMember es utilizada por ClusterNet para notificar la incorporacion
 * o eliminacion de un ID_Socket
 */
public class ClusterNetEventMember extends ClusterNetEvent
{

  /** ID_Socket */
  private ID_Socket id_socket = null;

  /** Boolean bAñadido. especifica si el ID_Socket ha sido añadido o eliminado */
  private boolean bAñadido = false;

  /**
   * Constructor ClusterNetEventMember
   * @param socket Un objeto SocketClusterNetImp
   * @param sInformativa cadena Informativa
   */
  public ClusterNetEventMember(SocketClusterNetImp socket,String sInformativa,
        ID_Socket id_socket,boolean bAñadido)
  {
    super(socket,EVENTO_ID_SOCKET,sInformativa);
    this.id_socket = id_socket;
    this.bAñadido = bAñadido;
  }

  /**
   * Obtiene el ID_Socket
   * @return el objeto ID_Socket
   */
  public ID_Socket getID_Socket(){return this.id_socket;}

  /**
   * Boolean que indica si el ID_Socket ha sido añadido o eliminado del grupo local
   */
  public boolean esAñadido() { return this.bAñadido;}

}

