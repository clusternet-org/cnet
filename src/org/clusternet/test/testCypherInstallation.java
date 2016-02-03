//  Historial: 
//	14/10/2014 Change Licence to LGPL
//
// 	Authors: 
//		 Alejandro Garcia Dominguez (alejandro.garcia.dominguez@gmail.com)
//		 Antonio Berrocal Piris (antonioberrocalpiris@gmail.com)
//
//
//      This file is part of ClusterNet 
//
//      ClusterNet is free software: you can redistribute it and/or modify
//      it under the terms of the Lesser GNU General Public License as published by
//      the Free Software Foundation, either version 3 of the License, or
//      (at your option) any later version.
//
//      ClusterNet is distributed in the hope that it will be useful,
//      but WITHOUT ANY WARRANTY; without even the implied warranty of
//      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//      Lesser GNU General Public License for more details.
//
//      You should have received a copy of the Lesser GNU General Public License
//      along with ClusterNet.  If not, see <http://www.gnu.org/licenses/>.

package org.clusternet.test;

import javax.crypto.*;

import org.clusternet.ClusterTimer;

import iaik.security.provider.IAIK;

public class testCypherInstallation
{

  public testCypherInstallation()
  {
  }

   /**
    * A simple testCypherInstallation for a correct installation.
   */
   public static void main(String arg[]) {

     IAIK.addAsProvider(true);

     try {
       Cipher cipher = Cipher.getInstance("DES", "IAIK");
     } catch (Exception ex) {
       System.out.println("Exception: "+ex.getMessage());
       System.out.println("\n\nIAIK-JCE installation error...");
       System.exit(0);
     }

     System.out.println("IAIK-JCE installation OK!");

     ClusterTimer.sleep(20000);
   }
  }

  
