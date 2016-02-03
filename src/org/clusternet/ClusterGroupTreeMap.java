//============================================================================
//
//	Copyright (c) 1999-2015 . All Rights Reserved.
//
//----------------------------------------------------------------------------
//
//	Fichero: ClusterGroupTreeMap.java  1.0 17/11/99
//
//
//	Descripción: ClusterGroupID
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
//----------------------------------------------------------------------------

package org.clusternet;

import java.util.TreeMap;

/**
 * Esta clase encapsula un objeto ClusterGroupID y otro TreeMap,
 * se utiliza como VALUE en el TreeMap treeMapIDGLVecinos
 * dentro de la clase CGLThread
 */
class ClusterGroupTreeMap
{
  /** ClusterGroupID */
  ClusterGroupID clusterGroupID = null;

  /** TreeMap */
  TreeMap treemap = null;

  ClusterGroupTreeMap(ClusterGroupID clusterGroupID, TreeMap treemap)
  {
    this.clusterGroupID = clusterGroupID;
    this.treemap = treemap;
  }
}
