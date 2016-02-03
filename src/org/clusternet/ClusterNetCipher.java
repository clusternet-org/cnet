//============================================================
//
//	Copyright (c) 1999-2015 . All Rights Reserved.
//
//------------------------------------------------------------
//
//	Fichero: ClusterNetCipher.java  1.0 24/11/99
//
//
//	Descripción: Interfaz ClusterNetCipher
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

/**
 * La interfaz ClusterNetCipher se utiliza para proporcionar seguridad al Channel Multicast.
 * Su única misión es proporcionar dos objetos javax.crypto.Cipher para codificar
 * y descodificar, además de un método de iniciación del codificador y del descodificador.
 * En este método se deberá llamar al método init() del los objetos javax.crypto.Cipher.
 */
public interface ClusterNetCipher
{

  public javax.crypto.Cipher getCipher();

  public javax.crypto.Cipher getUncipher();

  public void init();
}
