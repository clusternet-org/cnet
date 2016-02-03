//============================================================
//
//	Copyright (c) 1999-2015 . All Rights Reserved.
//
//------------------------------------------------------------
//
//	File: ClusterNetExcepcion.java  1.0 30/08/99
//
//
//	Descripción: Clase ClusterNetExcepcion. Excepción genérica para ClusterNet
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

import java.io.*;

/**
 * Clase ClusterNetExcepcion. Es una excepción genérica que devuelven diferentes
 * métodos. La excepción se lanza en condiciones de error, si el método
 * ha realizado su operación correctamente la excepción no es lanzada.
 * La excepción contiene un mensaje (opcional) que explica el error ocurrido.
 * @version 1.0
 */
public class ClusterNetExcepcion extends IOException {

  //==========================================================================
  /**
   * Constructor por defecto.
   */
  public ClusterNetExcepcion() { super();
  // PRUEBAS
  Log.log ("EXCEPCION: PTMFEXCEPCION. ","");


//  System.exit (1);
   }

  //==========================================================================
  /**
   * Constructor con un mensaje informativo del error ocurrido.
   * @param msg La cadena informativa.
   */
  public ClusterNetExcepcion(String msg) { super(msg);
  // PRUEBAS

  Log.log ("EXCEPCION: PTMFEXCEPCION. ",msg);

  //  System.exit (1);
   }

  //==========================================================================
  /**
   * Este constructor crea un objeto excepción ClusterNetExcepcion con un mensaje
   * informativo del error ocurrido, además imprime el mensaje en stdout.
   * @param mn Nombre del método que lanzó la excepción.
   * @param msg La cadena informativa.
   */
  public ClusterNetExcepcion(String mn,String msg) {
      super("["+mn+"] "+msg);
      Log.log("ClusterNetExcepcion: "+mn,msg);
  // PRUEBAS
  Log.log ("EXCEPCION: PTMFEXCEPCION. ",mn+" "+msg);

//  System.exit (1);

  }
}
