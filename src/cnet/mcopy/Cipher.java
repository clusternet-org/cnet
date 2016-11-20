//============================================================================
//
//	Copyright (c)2016 clusternet.org - All Rights Reserved.
//
//----------------------------------------------------------------------------
//
//	Description: Cipher
//
//History: 
//	09/02/2016 Create
//
//	Authors: 
//		 Alejandro Garcia Dominguez (alejandro.garcia.dominguez@gmail.com)
//		
//
//This file is part of ClusterNet 
//
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//	http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//----------------------------------------------------------------------------


package cnet.mcopy;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.NoSuchPaddingException;

import cnet.Log;


/** Clase Cipher. Crea los objetos de codificación y de decodificación. */
class Cipher{

  /** Objeto cifrador */
  javax.crypto.Cipher cipher = null;

  /** Objeto descifrador */
  javax.crypto.Cipher unCipher = null;

  /** runGUI */
  //runGUI ftp = null;

  private  byte[] salt = { (byte)0xe7, (byte)0x43, (byte)0x71, (byte)0xec,
    (byte)0x7e, (byte)0xb8, (byte)0xff, (byte)0x35 };

  // Número de iteraciones
  private int count = 20;

  //==========================================================================
 /**
  * Constructor Cipher protegido.
 * @throws NoSuchProviderException 
 * @throws NoSuchAlgorithmException 
 * @throws InvalidKeySpecException 
 * @throws NoSuchPaddingException 
 * @throws InvalidAlgorithmParameterException 
 * @throws InvalidKeyException 
  */
  protected Cipher(char[] clave) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException
  {
    // Salt

     //Log.log("ANTES DEL IF","");

     if(clave != null)
     {
       Log.log("Crypto --> Key:"+clave,"");
       // Criptografía --> JCE 1.2
       // Utilizar PKCS#5 para crear clave secreta.
        //Añadir proveedor..
        iaik.security.provider.IAIK.addAsProvider(true);

        // Establecer parámetros PBE
        javax.crypto.spec.PBEParameterSpec pbeParamSpec = new javax.crypto.spec.PBEParameterSpec(salt, count);

        // Covertir password en un objeto SecretKey, usando una llave PBE.
        javax.crypto.spec.PBEKeySpec pbeKeySpec = new javax.crypto.spec.PBEKeySpec(clave);
        //Log.log("DEPU 1","");

        javax.crypto.SecretKeyFactory secKeyFac = javax.crypto.SecretKeyFactory.getInstance("PBE","IAIK");//"PBEWithMD5AndDES");
        //Log.log("DEPU 2","");

        javax.crypto.SecretKey pbeKey = secKeyFac.generateSecret(pbeKeySpec);
        //Log.log("DEPU 3","");

        // Crear los objetos cipher
        cipher = javax.crypto.Cipher.getInstance("RC2/ECB/PKCS5Padding");//"PbeWithMD5AndDES_CBC");
        unCipher = javax.crypto.Cipher.getInstance("RC2/ECB/PKCS5Padding");
        //Log.log("DEPU 4","");

        // Inicializar los cifradores con la llave y los parámetros...
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);
        unCipher.init(javax.crypto.Cipher.DECRYPT_MODE, pbeKey, pbeParamSpec);
        //Log.log("DEPU 5","");
       
     

    }
  }

 //==========================================================================
 /**
  * Devuleve el objeto cifrador
  */
  javax.crypto.Cipher getCipher(){ return this.cipher;}

 //==========================================================================
 /**
  * Devuleve el objeto descifrador
  */
  javax.crypto.Cipher getUncipher(){ return this.unCipher;}


 //==========================================================================
 /**
  * Obtien un objeto Cipher.
 * @throws InvalidAlgorithmParameterException 
 * @throws NoSuchPaddingException 
 * @throws InvalidKeySpecException 
 * @throws NoSuchProviderException 
 * @throws NoSuchAlgorithmException 
 * @throws InvalidKeyException 
  */
  static  Cipher getInstance(char[] clave) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, NoSuchPaddingException, InvalidAlgorithmParameterException
  {
     return new Cipher(clave);
  }
}