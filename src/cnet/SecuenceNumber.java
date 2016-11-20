//============================================================
//
//	Copyright (c) 1999-2015 . All Rights Reserved.
//
//------------------------------------------------------------
//
//	File: SecuenceNumber.java  1.0
//
//	Descripción: Clase SecuenceNumber. Operaciones y almacen para
//                   manejar un número de secuencia.
//
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

package cnet;

import java.lang.Cloneable;
import java.lang.Comparable;


/**
 * Clase que almacena un número de secuencia y ofrece las operaciones
 * necesarias para su manejo. <br>
 * Un número de secuencia está formado por un entero de 32 bits (sin signo)
 * que identifica un {@link TPDUDatosNormal} enviado por un id_socket.<br>
 *
 * <b>Una vez creado no puede ser modificado.</b>
 * @version 1.0
 * @author Antonio Berrocal Piris
 * <A HREF="mailto:AntonioBP@wanadoo.es">(AntonioBP@wanadoo.es)</A><p>
 * M. Alejandro García Domínguez
 * <A HREF="mailto:alejandro.garcia.dominguez@gmail.com">(alejandro.garcia.dominguez@gmail.com)</A><p>
 */
public class SecuenceNumber implements Cloneable,Comparable

{

 /** Almacena el valor del número de secuencia. */
  private long lNumeroSecuencia = 1;

  //=================================================================
  /**
   * Crea un objeto número de secuencia con el valor indicado.
   * @param numSec valor del número de secuencia
   * @exception ClusterNetInvalidParameterException lanzada si el número de secuencia no es
   * válido.
   */
   public SecuenceNumber (long lNumSec) throws ClusterNetInvalidParameterException
   {
    final String mn = "SecuenceNumber.Numerosecuencia(long)";
    if (esValido (lNumSec))
        this.lNumeroSecuencia = lNumSec;
    else throw new ClusterNetInvalidParameterException ("El número de secuencia " + lNumSec +
                                                " no es válido.");
   }

  //=================================================================
  /**
   * Comprueba que el número de secuencia indicado es válido, para lo cual
   * debe ser mayor o igual a cero.
   * @param numSec número de secuencia
   * @return true si el número de secuencia es válido, false en caso contrario.
   */
  private boolean esValido (long lNumSec)
  {
   if (lNumSec < 0)
        return false;
   return true;
  }

  //=================================================================
  /**
   * Devuelve el número de secuencia como un long.
   * @return número de secuencia como long
   */
  public long tolong()
  {
     return this.lNumeroSecuencia;
  }

   //=================================================================
   /**
    * Implementa el método de la interfaz {@link Comparable}.
    * @param o número de secuencia para comparar con este, si no es una
    * instancia de {@link SecuenceNumber} se lanzará la excepción
    * {@link java.lang.ClassCastException}.
    * @return -1 si este número de secuencia es menor que el dado,
    * 1 si es mayor y 0 si son iguales.
    */
   public int compareTo(Object o)
   {
    SecuenceNumber ns = (SecuenceNumber) o;

    if (this.lNumeroSecuencia<ns.tolong())
        return -1;
    if (this.lNumeroSecuencia>ns.tolong())
        return 1;
    return 0;
   }

   //=================================================================
   /**
    * Comprueba si este número de secuencia es igual al pasado por parámetro.
    * @return true si son iguales, y false en caso contrario.
    */
   public boolean equals (Object o)
   {
    SecuenceNumber ns = (SecuenceNumber) o;

    if (this.lNumeroSecuencia==ns.tolong())
        return true;
    return false;
   }

   //=================================================================
   /**
    * Comprueba si este número de secuencia es igual al pasado por parámetro.
    * @return true si son iguales, y false en caso contrario.
    */
   public boolean igual (SecuenceNumber o)
   {
    if (this.compareTo(o)==0)
        return true;
    return false;
   }

   //=================================================================
   /**
    * Comprueba si este número de secuencia es mayor o igual al pasado por parámetro.
    * @return true si son iguales, y false en caso contrario.
    */
   public boolean mayorIgual (SecuenceNumber o)
   {
    if (this.compareTo(o)>=0)
        return true;
    return false;
   }

   //=================================================================
   /**
    * Comprueba si este número de secuencia es mayor al pasado por parámetro.
    * @return true si es mayor, y false en caso contrario.
    */
   public boolean mayor (SecuenceNumber o)
   {
    if (this.compareTo (o)>0)
        return true;
    return false;
   }

   //=================================================================
   /**
    * Comprueba si este número de secuencia es menor al pasado por parámetro.
    * @return true si es menor, y false en caso contrario.
    */
   public boolean menor (SecuenceNumber o)
   {
    if (this.compareTo (o)<0)
        return true;
    return false;
   }

   //=================================================================
   /**
    * Comprueba si este número de secuencia es menor o igual al pasado por parámetro.
    * @return true si es menor, y false en caso contrario.
    */
   public boolean menorIgual (SecuenceNumber o)
   {
    if (this.compareTo (o)<=0)
        return true;
    return false;
   }

   //=================================================================
   /**
    * Devuelve el número de secuencia siguiente.
    * @return número de secuencia siguiente
    */
   public long getSiguiente ()
   {
     return (this.lNumeroSecuencia+1);
   }

   //=================================================================
   /**
    * Devuelve el número de secuencia anterior, o 0 si no existe.
    * @return número de secuencia anterior, o 0 si no existe.
    */
   public SecuenceNumber getAnterior () throws ClusterNetExcepcion
   {
    try {
      if (this.lNumeroSecuencia>0)
            return new SecuenceNumber (this.lNumeroSecuencia-1);
     }catch (ClusterNetInvalidParameterException e)
         {throw new ClusterNetExcepcion (e.toString());}

     throw new ClusterNetExcepcion ("El número de secuencia no es válido.");
   }

   //=================================================================
   /**
    * Incrementa el número de secuencia en la cantidad indicada.
    * Si cantidad es menor que cero, no se incrementa.
    * @param iCantidad en que se incrementa el número de secuencia
    * @exception ClusterNetInvalidParameterException
    */
   public SecuenceNumber incrementar (long lCantidad)
      throws ClusterNetInvalidParameterException
   {
    return new SecuenceNumber (this.lNumeroSecuencia+lCantidad);
   }

   //=================================================================
   /**
    * Decrementa el número de secuencia en una unidad si es mayor que cero.
    * @throws  ClusterNetExcepcion
    */
   public SecuenceNumber decrementar ()
      throws ClusterNetExcepcion
   {
    // Al crearse el nuevo número se lanza la excepción si no es válido.
    try{
      return new SecuenceNumber (this.lNumeroSecuencia-1);
     }catch (ClusterNetInvalidParameterException e)
        {throw new ClusterNetExcepcion (e.toString());}
   }

   //=================================================================
   /**
    * Crea un copia de este número de secuencia. Lo que devulve no es una referencia
    * a este objeto, sino un nuevo objeto cuyos datos son copias de este.
    * @return número de secuencia clon de este.
    */
   public Object clone()
   {
    final String mn = "SecuenceNumber.clone";
     try{
       return new SecuenceNumber (this.lNumeroSecuencia);
      }catch (ClusterNetInvalidParameterException e)
             {}
     return null;

   }

   //=================================================================
   /**
    * Devuelve el número de secuencia mayor posible (LIMITESUPERIOR).
    */
   public static SecuenceNumber LIMITESUPERIOR;

   static {
    try
    {
     LIMITESUPERIOR = new SecuenceNumber (Long.MAX_VALUE-1);
    }
    catch(java.lang.Exception e)
    {
      ;
    }
   }

   //=================================================================
   /**
    * Devuelve el número de secuencia menor posible (LIMITEINFERIOR).
    */
   public static SecuenceNumber LIMITEINFERIOR;

   static {
     try
     {
      LIMITEINFERIOR = new SecuenceNumber (1);
     }
     catch(java.lang.Exception e)
     {
      ;
     }
   }

   //=================================================================
   /**
    * Devuelve una cadena representación del número de secuencia.
    * @return cadena representación del número de secuencia.
    */
   public String toString ()
   {
    return "NSec: " + this.lNumeroSecuencia;
   }




}
