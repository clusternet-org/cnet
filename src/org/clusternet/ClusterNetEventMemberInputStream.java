//============================================================
//
//	Copyright (c) 1999-2015 . All Rights Reserved.
//
//------------------------------------------------------------
//
//	File: ClusterNetEventMemberInputStream.java  1.0 13/04/2000
//
//
//	Descripción: Clase ClusterNetEventMemberInputStream. Evento ClusterNet ID_Socket
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
//
//------------------------------------------------------------

package org.clusternet;

import java.util.EventObject;

/**
    La clase ClusterNetEventMemberInputStream es un evento ClusterNet utilizado
    por la clase ClusterNetInputStream para notificar a la aplicación
    cuando hay un NUEVO FLUJO DE ENTRADA ID_SocketInputStream.<br>
    El evento lleva asociado el nuevo flujo ID_SocketInputStream añadido.
 */
public class ClusterNetEventMemberInputStream extends ClusterNetEvent
{

  /** ID_SocketInputStream */
  private ID_SocketInputStream id_socketInputStream = null;


  /**
   * Constructor ClusterNetEventMemberInputStream
   * @param socket Un objeto SocketClusterNetImp
   * @param sInformativa cadena Informativa
   */
  public ClusterNetEventMemberInputStream(SocketClusterNetImp socket,String sInformativa,
        ID_SocketInputStream id_socketInputStream)
  {
    super(socket,EVENTO_ID_SOCKET,sInformativa);
    this.id_socketInputStream = id_socketInputStream;

  }

  /**
   * Obtiene el ID_SocketInputStrem
   * @return el objeto ID_SocketInputStream
   */
  public ID_SocketInputStream getID_SocketInputStream(){return this.id_socketInputStream;}

}

