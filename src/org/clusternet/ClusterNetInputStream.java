//============================================================================
//
//	Copyright (c) 1999-2015. All Rights Reserved.
//
//----------------------------------------------------------------------------
//
//	File: ClusterNetInputStream.java  1.0 24/11/99
//
//	Description: Clase ClusterNetInputStream.
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

import java.io.InputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * ClusterNetInputStream es un "GESTOR" de flujos de entrada
 * ClusterMemberInputStream para el ClusterNet. <br>
 * Existe un objeto ClusterMemberInputStream por cada emisor.
 * @version  1.0
 * @author M. Alejandro García Domínguez
 * <A HREF="mailto:alejandro.garcia.dominguez@gmail.com">(alejandro.garcia.dominguez@gmail.com)</A><p>
 *			   Antonio Berrocal Piris
 */
public class ClusterNetInputStream implements Enumeration
{
  /** ClusterNet   */
  private SocketClusterNetImp socket = null;

  /** Iterador para los flujos */
  private Iterator  iterator = null;


  //==========================================================================
  /**
   * Constructor. Crea un flujo de salida MulticastInpustStream.
   * <br> Un "gestor" de flujos ID_SocketImputStream
   * @param socket El objeto SocketClusterNetImp
   */
  ClusterNetInputStream(SocketClusterNetImp socket)
  {
   super();

   this.socket = socket;

   if(this.socket == null)
    throw new NullPointerException("ClusterNetInputStream: Parámetro colaRecepción nulo.");
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
   this.socket.addPTMFID_SocketInputStreamListener (obj);
  }

  //==========================================================================
  /**
   * Elimina un objeto ClusterNetMemberInputStreamListener
   * @param obj el objeto ID_SocketInputStreamListener a eliminar
   */
  public void removePTMFID_SocketInputStreamListener(ClusterNetMemberInputStreamListener obj)
  {
    this.socket.removePTMFID_SocketInputStreamListener (obj);
  }

  //==========================================================================
  /**
   * Un método para registrar un objeto que implementa la interfaz ClusterNetRxDataListener.
   * La interfaz ClusterNetRxDataListener se utiliza para notificar a las clases que se
   * registren de un evento ClusterNetEventNewData
   * @param obj El objeto ClusterNetRxDataListener
   * @return ClusterNetExcepcion Se lanza cuando ocurre un error al registrar el
   *  objeto callback
   */
  public void addPTMFDatosRecibidosListener(ClusterNetRxDataListener obj)
  {
    this.socket.addPTMFDatosRecibidosListener(obj);
  }

  //==========================================================================
  /**
   * Elimina un objeto ClusterNetRxDataListener
   * @param obj El objeto que implementa la interfaz ClusterNetRxDataListener
   */
  public void removePTMFDatosRecibidosListener(ClusterNetRxDataListener obj)
  {
    this.socket.removePTMFDatosRecibidosListener(obj);
  }

  //==========================================================================
  /**
   * Devuelve el número de bytes que pueden ser leídos (o descartados) de
   * este flujo de entrada sin bloquear por la siguiente llamada a un método
   * de lectura.
   * @return El número de bytes que pueden ser leídos desde este flujo de entrada.
   * @exception IOException - si ocurre un error de I/O
   */
  public int available() throws IOException
  {
    return this.socket.getColaRecepcion().getTamaño();
  }

  //==========================================================================
  /**
   * Devuelve lo mismo que hasMoreID_SocketInputStream()
   * @return true si hay más ClusterMemberInputStream con datos para leer y false en caso contrario.
   */
  public boolean hasMoreElements()
  {
    return this.hasMoreID_SocketInputStream();
  }

  //==========================================================================
  /**
   * Comprueba si hay más ClusterMemberInputStream con datos para leer.
   * @return true si hay más ClusterMemberInputStream con datos para leer y false en caso contrario.
   */
  public boolean hasMoreID_SocketInputStream()
  {
    try
    {
     if(this.available() <= 0)
        return false;
     else
        return true;
    }
    catch(IOException e){ return false;}
  }

  //==========================================================================
  /**
   * Devuelve lo mismo que nextID_SocketInputStream().
   * @return Un objeto ClusterMemberInputStream que contiene datos que leer.
   */
  public Object nextElement() throws NoSuchElementException
  {
   return this.nextID_SocketInputStream();
  }


  //==========================================================================
  /**
   * Devuelve el siguiente flujo de entrada ClusterMemberInputStream listo para leer
   * datos.
   * Si no hay ningún flujo con datos disponible se lanza la excpeción NoSuchElementException.
   * @return un objeto ClusterMemberInputStream que contiene datos que leer.
   */
  public ClusterMemberInputStream nextID_SocketInputStream() throws NoSuchElementException
  {
    //Ver primero si hay datos, si no "pa" que vamos a devolver un flujo.
    try
    {
     if(this.available() <= 0)
        throw new NoSuchElementException();
    }
    catch(IOException e){ ;}

    return this.socket.getColaRecepcion().nextID_SocketInputStream();
  }



  //==========================================================================
  /**
   * Cierra este flujo y elimina todos los recursos asociados a él
   * Una vez cerrado el flujo no se podrá volver a utilizarlo.
   */
  public void close()
  {
   Iterator i = this.socket.getColaRecepcion().getTreeMap().values().iterator();

   try
   {
    while(i.hasNext())
     {
        ClusterMemberInputStream id = (ClusterMemberInputStream) i.next();
        id.close();
     }

     //this.socketPTMFImp.getColaRecepcion() = null;
     this.iterator = null;
   }
   /*catch(IOException e)
   {
    return;
   }
   */
   finally
   {
    ;
   }
  }

}
