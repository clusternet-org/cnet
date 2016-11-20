//============================================================================
//
//	Copyright (c)2016 clusternet.org - All Rights Reserved.
//
//----------------------------------------------------------------------------
//
//	Description: CCopyEvent
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

import cnet.ClusterMemberID;

/**
 * Event used in InterfaceCCopy to pass information using callbaks.
 * @author Alejandro Garcia
 *
 */
public class CCopyEvent {

	public int type;
	public String file;
	
	public long size;
	public long transmitted;
	public ClusterMemberID member;
	
	/**
	 * @param type
	 * @param file
	 * @param size
	 * @param progress
	 */
	public CCopyEvent(int type, String file, long size, long lBytesTransmitidos,ClusterMemberID member) {
		super();
		this.type = type;
		this.file = file;
		this.size = size;
		this.transmitted = lBytesTransmitidos;
		this.member = member;
	}
}
