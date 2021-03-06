//============================================================================
//
//	Copyright (c) 1999-2015 . All Rights Reserved.
//
//----------------------------------------------------------------------------
//
//	File: IDGL.java  1.0 21/10/99
//
//
//	Description: IDGL
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

import java.util.Comparator;

/**
 * IDGL encapsula el identificador del grupo local:
 *  IDGL = IPv4 + Puerto Unicast
 *
 *
 * y el TTL que indica la distancia hacia ese grupo local.
 * @version  1.0
 * @author M. Alejandro Garc�a Dom�nguez
 * <A HREF="mailto:alejandro.garcia.dominguez@gmail.com">(alejandro.garcia.dominguez@gmail.com)</A><p>
 *			   Antonio Berrocal Piris
 */
public class IDGL implements Cloneable ,Comparable{

  /** identificador de grupo local*/
  Buffer id = null;

  /** TTL del identificador de grupo local */
  short TTL = 0;

  /** String ID */
  private String sID = null;

  /** HashCode */
  private int iHashCode = 0;

  //==========================================================================
  /**
   * Constructor.
   * @param id Buffer de bytes del identificador de grupo
   * @param ttl TTL del id de grupo.
   */
  IDGL(Buffer id, byte ttl)
  {
    super();

    this.id = id;
    if(id.getMaxLength() < 6)
    {
      //Error fatal.
      Log.log("IDGL()","ERROR FATAL: IDGL menor de 6 bytes.");
      Log.exit(-1);
    }

    this.TTL = ttl;
    iHashCode = getHashCode();
    sID = getsID();

  }

  //==========================================================================
  /**
   * Constructor sin par�metros.
   * Crea un id 0.0.0.0.0.0 y un TTL 0
   */
 /* IDGL()
  {
    super();

    try
    {
      this.id = new Buffer(6);
      id.addByte((byte)0,0);
      id.addByte((byte)0,1);
      id.addByte((byte)0,2);
      id.addByte((byte)0,3);
      id.addByte((byte)0,4);
      id.addByte((byte)0,5);
    }
    catch(ClusterNetExcepcion e){;}
    catch(ClusterNetInvalidParameterException e){;}

    this.TTL = 0;

    iHashCode = getHashCode();
    sID = getsID();

  }
   */
  //==========================================================================
  /**
   * Implementaci�n de la interfaz Cloneable. M�todo clone
   * @return El nuevo objeto clonado.
   */
  public Object clone()
  {
     return new IDGL((Buffer)this.id.clone(),(byte)this.TTL);
  }

  //==========================================================================
  /**
   * Devuelve el identificador de grupo como una cadena x.x.x.x.X.X
   * @return Una cadena del identificador de grupo en formato x.x.x.x.X.X
   */
   String getsID()
   {
     short A = 0;
     short B = 0;
     short C = 0;
     short D = 0;
     short P1 = 0;
     short P2 = 0;

     try
     {
       A = this.id.getByte(0);
       B = this.id.getByte(1);
       C = this.id.getByte(2);
       D = this.id.getByte(3);
       P1 = this.id.getByte(4);
       P2 = this.id.getByte(5);
     }
     catch(ClusterNetInvalidParameterException e)
     {
      ;
     }

     //Obtenci�n del puerto
     int puerto = P1;
     puerto = puerto << 8;
     puerto |= P2;

     String ID = A+"."+B+"."+C+"."+D+":"+puerto; //P1+"."+P2  ;

     return ID;
   }

  //==========================================================================
  /**
   * Devuelve el identificador de grupo como una cadena x.x.x.x.X.X
   * @return Una cadena del identificador de grupo en formato x.x.x.x.X.X
   */
   String getStringID() { return this.sID;}

  //==========================================================================
  /**
   * Devuelve el c�digo hash para este IDGL.
   * El c�digo hash se basa s�lo en el identificador del grupo local.
   * @return el c�digo hash para el IDGL
   */
  public int hashCode(){ return this.iHashCode;}

  //==========================================================================
  /**
   * Devuelve el c�digo hash para este IDGL.
   * El c�digo hash se basa s�lo en el identificador del grupo local.
   * @return el c�digo hash para el IDGL
   */
  public int getHashCode()
  {
    try
    {
     if ( (this.id != null) && (this.id.getBuffer() != null))
      return ((int)this.id.getInt(2));
     else
       return 0;

    }

    catch(ClusterNetInvalidParameterException e){;}

    return 0;
  }

  //==========================================================================
  /**
   * Este m�todo verifica si el objeto pasado como argumento es igual a este
   * objeto. Se comparan el TTL y el identificador del grupo local.
   * @param obj Objeto a comparar con este.
   * @return true si el objeto es igual, false en caso contrario.
   */
  public boolean equals(Object obj)
  {
    IDGL idgl = (IDGL) obj;

    //if (this.TTL == idgl.TTL)
    //{
      for (int i=0; i< 6; i++)
       if (this.id.getBuffer()[i] != idgl.id.getBuffer()[i])
        return false;

      return true;
    //}
    //return false;
  }

  //==========================================================================
  /**
   * Implementaci�n del m�todo de la interfaz Comparable.
   * Compara primero la direcci�n y despu�s el n�mero de secuencia.
   * @param o IDGL con la que se compara.
   * @return mayor que cero si este IDGL es mayor que el pasado en el
   * argumento, menor que cero si es menor y cero si son iguales.
   */
 public int compareTo(Object o)
 {
    int hash1 = 0;
    int hash2 = 0;
    IDGL idgl = (IDGL) o;

    hash1 = this.hashCode();
    hash2 = idgl.hashCode();

    if ( hash1 < hash2 )
        return -1;
    else if ( hash1 > hash2)
        return 1;
    else return 0;

 }
  //==========================================================================
  /**
   * Este m�todo compara dos objetos pasados como argumentos.
   * @param o1 Objeto 1 a comparar
   * @param o2 Objeto 2 a comparar
   * @return Devuelve un entero negativo, cero o un entero positivo si el
   *   primer argumento es menor que, igual a, o mayor que el segundo.
   */
  public int compare(Object o1, Object o2)
  {
    int hash1 = 0;
    int hash2 = 0;

    hash1 = o1.hashCode();
    hash2 = o2.hashCode();

    if ( hash1 < hash2 )
        return -1;
    else if ( hash1 > hash2)
        return 1;
    else return 0;
  }

  //==========================================================================
  /**
   * DEVUELVE IDGL COMO UN LONG PARA A�ADIR A LOS TPDU
   */
  public long getIDGL ()
  {
   return this.hashCode();
  }

  //==========================================================================
  /**
   * Devuelve una cadena identificativa del objeto.
   * @return Cadena Indentificativa.
   */

  public String toString ()
  {
   return ("[IDGL: "+getStringID()+"] TTL="+this.TTL);
  }

}
