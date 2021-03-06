//============================================================================
//
//	Copyright (c) 1999-2015 . All Rights Reserved.
//
//----------------------------------------------------------------------------
//
//	Fichero: RegistroIDGL_TreeMap.java  1.0 24/11/99
//
//
//	Descripci�n: Clase RegistroIGDL_TreeMap
//
// 	
// 	Authors: 
//		 Alejandro Garc�a-Dom�nguez (alejandro.garcia.dominguez@gmail.com)
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

package cnet;

import java.util.TreeMap;

/**
 * <p>Title: ClusterNet v1.1</p>
 * <p>Description: Protocolo de Transporte Multicast Fiable</p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.1
 */


  /**
   * Clase RegistroIDGL_TreeMap.<br>
   * Almacena un ClusterGroupID y un TreMap con ClusterGroupID.
   */
   public class RegistroIDGL_TreeMap
   {
      ClusterGroupID clusterGroupID = null;
      TreeMap treemap = null;

      RegistroIDGL_TreeMap(ClusterGroupID clusterGroupID,TreeMap treemap)
      {
       this.clusterGroupID = clusterGroupID;
       this.treemap = treemap;
      }

      /**
       * Obtiene el ClusterGroupID
       * @return
       */
      public ClusterGroupID getIDGL()
      {
        return clusterGroupID;
      }

      /**
       * Obtiene el treemap
       * @return
       */
      public TreeMap getTreeMap()
      {
        return treemap;
      }
   }