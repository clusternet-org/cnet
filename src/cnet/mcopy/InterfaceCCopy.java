//============================================================================
//
//	Copyright (c)2016 clusternet.org - All Rights Reserved.
//
//----------------------------------------------------------------------------
//
//	Description: Interface for callbacks from CCopyImp
//
//  History: 
//	09/02/2016 Create
//
// 	Authors: 
//		 Alejandro Garcia Dominguez (alejandro.garcia.dominguez@gmail.com)
//		
//
//  This file is part of ClusterNet 
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//	http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//----------------------------------------------------------------------------
package cnet.mcopy;

import java.util.TreeMap;

/**
 *  Interface for callbacks from CCopyImp
 *
 */
public interface InterfaceCCopy {

	/** Notify numbers groups in clusterNet */
	void notifyGroups(int numGroups);

	/** Notify numbers members in local group in clusterNet */
	void notifyMembers(int numMembers);
	
	/** Notify groups in clusterNet */
	void notifyGroups(TreeMap treemapGroups);

	/** Notify members in local group in clusterNet */
	void notifyMembers(TreeMap treemapMembers);

	/** Indicate connection's status of clusterNet CCopy protocol*/
	void statusClusterConnection(CCopyEvent event);

	/** This client will send files=true (not allowed to be a receiver at same time)*/
	boolean isSender();

	/** Print info log */
	void infoLog(String string);
	
	/** Print error log */
	void errorLog(String string);


	
	
	
}
