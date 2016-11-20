//============================================================================
//
//	Copyright (c) 1999-2015. All Rights Reserved.
//
//----------------------------------------------------------------------------
//
//	Fichero: TPDUMACK.java  1.0 9/9/99
//
//
//	Descripci�n: Clase TPDUMACK.
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
import java.util.*;
import java.lang.*;


/**
 * Clase TPDU MACK.<br>
 * Hereda de la clase TPDUDatos.<br>
 *
 * Para crear un objeto de esta clase se tienen que usar los m�todos est�ticos.
 * Una vez creado no puede ser modicado.<br>
 *
 * El formato completo del TPDU MACK es: <br>
 * <br>
 *                      1 1 1 1 1 1 1 1 1 1 2 2 2 2 2 2 2 2 2 2 3 3<br>
 * +0-1-2-3-4-5-6-7-8-9-0-1-2-3-4-5-6-7-8-9-0-1-2-3-4-5-6-7-8-9-0-1<br>
 * +---------------------------------------------------------------+<br>
 * +      Puerto Multicast         |          Puerto Unicast       +<br>
 * +---------------------------------------------------------------+<br>
 * +                        ID_GRUPO_LOCAL                         +<br>
 * +                      (4 bytes primeros)                       +<br>
 * +---------------------------------------------------------------+<br>
 * +        ID_GRUPO_LOCAL         |          Longitud             +<br>
 * +      (2 bytes �ltimos)        |                               +<br>
 * +---------------------------------------------------------------+<br>
 * +           Cheksum             | V |0|1|0|1|0|    No Usado     +<br>
 * +---------------------------------------------------------------+<br>
 * +     N�mero de R�faga Fuente   |      Direcci�n IP Fuente      +<br>
 * +                               |      (2 bytes primeros)       +<br>
 * +---------------------------------------------------------------+<br>
 * +      Direcci�n IP Fuente      |     Puerto Unicast Fuente     +<br>
 * +      (2 bytes �ltimos)        |                               +<br>
 * +---------------------------------------------------------------+<br>
 * +              N�mero de secuencia inicial r�faga               +<br>
 * +---------------------------------------------------------------+<br>
 * <br> 
 * <br>
 * Esta clase no es thread-safe.<br>
 * @see      Buffer
 * @version  1.0
 * @author M. Alejandro Garc�a Dom�nguez
 * <A HREF="mailto:alejandro.garcia.dominguez@gmail.com">(alejandro.garcia.dominguez@gmail.com)</A><p>
 * Antonio Berrocal Piris
 * <A HREF="mailto:AntonioBP.wanadoo.es">(AntonioBP@wanadoo.es)</A><p>
 */

public class TPDUMACK extends TPDUDatos
{
  // ATRIBUTOS
  /** Tama�o de la cabecera del TPDUHNACK*/
   static final int LONGHEADER = 7 * 4;

  /**
   * Direcci�n IP Fuente (32 bits)  : direcci�n IP del socket que origin� los datos.
   */
   IPv4 DIR_IP_FUENTE = null;

  /**
   * Puerto Unicast Fuente (16 bits): puerto unicast fuente
   */
   int PUERTO_UNICAST_FUENTE = 0;

  /**
    * N�mero de R�faga Fuente (16 bits): n�mero de r�faga para la que queremos
    * ser CG Local.
    */
   int NUMERO_RAFAGA_FUENTE = 0;

  /**
   * N�mero de secuencia Fuente de inicio de la r�faga.
   */
   SecuenceNumber N_SEC_INICIAL_RAFAGA = null;

  /**
   * Se forma con el valor de otros campos.<br>
   *    <ul>ID Socket Fuente (6 byte)
   *       <li>Direcci�n IP Fuente (4 byte)</li>
   *       <li>Puerto Unicast Fuente (2 byte)</li>
   *    </ul>
   */
   private ClusterMemberID ID_SOCKET_FUENTE = null;

  //==========================================================================
  /**
   * Constructor utilizado para crear un TPDUHACK.
   * @param socketClusterNetImp Objeto SocketClusterNetImp del que obtiene el valor de los
   * campos de la cabecera com�n.
   * @exception ClusterNetExcepcion
   * @exception ClusterNetInvalidParameterException lanzada si socketClusterNetImp es null
   */
  private TPDUMACK (SocketClusterNetImp socketClusterNetImp)
    throws ClusterNetExcepcion,ClusterNetInvalidParameterException
  {
   super (socketClusterNetImp);
  }




   * Constructor por defecto.
   * Este constructor es para crear TPDUS a partir del parser de un Buffer
   * @exception ClusterNetExcepcion
   * @exception ParametroInvalido Se lanza en el constructor de la clase TPDU.
   */
  private TPDUMACK ()





 //============================================================================
 /**
  * Crea un TPDUHACK con la informaci�n facilitada.
  * @param socketClusterNetImp Objeto SocketClusterNetImp del que obtiene el valor de los
  * campos de la cabecera com�n.
  * @param dirIPFuente direcci�n IP Fuente
  * @param nSec n�mero secuencia
  * @param puertoUnicastFuente puerto unicast fuente
  * @param numero_rafaga n�mero de r�faga fuente
  * @return objeto TPDUHACK creado.
  * @exception ClusterNetInvalidParameterException si alguno de los par�metros es err�neo.
  * @exception ClusterNetExcepcion si hay un error al crear el TPDUHACK

 static TPDUMACK crearTPDUMACK (SocketClusterNetImp socketClusterNetImp,
                                IPv4 dirIPFuente,
                                int numeroRafagaFuente,
                                int puertoUnicastFuente,
                                SecuenceNumber nSecInicialRafaga)
   throws ClusterNetInvalidParameterException, ClusterNetExcepcion
 {

   // Crear el TPDUDatos vacio
   TPDUMACK resultTPDU = new TPDUMACK (socketClusterNetImp);

   // Guardar los datos en la cabecera para cuando sean pedidos
   resultTPDU.DIR_IP_FUENTE         = dirIPFuente;
   resultTPDU.NUMERO_RAFAGA_FUENTE  = numeroRafagaFuente;
   resultTPDU.PUERTO_UNICAST_FUENTE = puertoUnicastFuente;
   resultTPDU.N_SEC_INICIAL_RAFAGA  = nSecInicialRafaga;

   return resultTPDU;
 }


  //==========================================================================
  /**
   * Construir el TPDU HACK, devuelve un buffer con el contenido del TPDUHACK,
   * seg�n el formato especificado en el protocolo.
   * <b>Este buffer no debe de ser modificado.</B>
   * @return un buffer con el TPDUHACK.
   * @exception ClusterNetExcepcion Se lanza si ocurre alg�n error en la construcci�n
   * del TPDU
   * @exception ClusterNetInvalidParameterException lanzada si ocurre alg�n error en la
   * construcci�n del TPDU
   */
 Buffer construirTPDUMACK () throws ClusterNetExcepcion,ClusterNetInvalidParameterException
 {
   final String mn = "TPDU.construirTPDUDatos";
   int offset = 14;


   // Crear la cabecera com�n a todos los TPDU
   Buffer bufferResult = construirCabeceraComun (ClusterNet.SUBTIPO_TPDU_DATOS_MACK,
                                                 TPDUMACK.LONGHEADER);

   if (bufferResult == null)
    throw new ClusterNetExcepcion (mn + "Buffer nulo");

   // 15� BYTE : Subtipo: (3 bits )
   short anterior = bufferResult.getByte (offset);
   // anterior : XXXX XXXX
   //      and : 1111 1110 = 0xFE
   //           ----------
   //            XXXX XXX0
   anterior &= 0xFE;
   bufferResult.addByte((byte)anterior,offset);
   offset++;






   bufferResult.addShort (this.NUMERO_RAFAGA_FUENTE,offset);
   offset+=2;

   // 19�, 20�, 21� y 22� BYTE : Direcci�n IP fuente
   bufferResult.addBytes (this.DIR_IP_FUENTE.ipv4,0,offset,4);
   offset+=4;

   // 23� y 24� BYTE : Puerto unicast fuente
   bufferResult.addShort (this.PUERTO_UNICAST_FUENTE,offset);
   offset+=2;

   // 25�, 26�, 27� y 28� BYTE : N�mero de Sec. Inicial de la r�faga.
   bufferResult.addInt (this.N_SEC_INICIAL_RAFAGA.tolong(),offset);
   offset+=4;

   return bufferResult;

}

  //==========================================================================
  /**
   * Parse un Buffer de datos recibidos y crea un TPDU HACK que lo encapsule.
   * El buffer debe de contener un TPDU HACK.
   * @param buf Un buffer que contiene el TPDU HACK recibido.
   * @param ipv4Emisor direcci�n IP unicast del emisor.
   * @return Un objeto TPDUHACK
   * @exception ClusterNetExcepcion El buffer pasado no contiene una cabecera TPDU
   * correcta, el mensaje de la excepci�n especifica el tipo de error.
   * @exception ClusterNetInvalidParameterException Se lanza si el buffer pasado no
   * contiene un TPDUHACK v�lido.
   */
 static  TPDUMACK parserBuffer (Buffer buffer,IPv4 ipv4Emisor)
   throws ClusterNetExcepcion,ClusterNetInvalidParameterException
 {
  final String mn = "TPDUMACK.parserBuffer (buffer,ipv4)";

  int aux;
  int offset = 16;

  if (buffer==null)
     throw new ClusterNetInvalidParameterException (mn+ "Buffer nulo");

  if (TPDUMACK.LONGHEADER > buffer.getMaxLength())
     throw new ClusterNetInvalidParameterException (mn+ "Buffer incorrecto");


  // Crear el TPDUDatos.
  TPDUMACK tpduMACK = new TPDUMACK ();

  // Analizar los datos comunes
  TPDUDatos.parseCabeceraComun (buffer,tpduMACK,ipv4Emisor);

  // Comprobar si el tipo es correcto
  if (tpduMACK.SUBTIPO != ClusterNet.SUBTIPO_TPDU_DATOS_MACK)
      throw new ClusterNetExcepcion (mn + "Subtipo de TPDU Datos no es MACK");


  //
  // 17� y 18� BYTE : N�mero de r�faga fuente
  //
  tpduMACK.NUMERO_RAFAGA_FUENTE = buffer.getShort (offset);
  offset+=2;

  //
  // 19�, 20�, 21� y 22� BYTE : Direcci�n IP fuente
  //
  tpduMACK.DIR_IP_FUENTE = new IPv4 (new Buffer (buffer.getBytes (offset,4)));
  offset+=4;

  //
  // 23� y 24� BYTE : Puerto Unicast Fuente
  //
  tpduMACK.PUERTO_UNICAST_FUENTE = buffer.getShort (offset);
  offset+=2;

  //
  // 25�, 26�, 27� y 28� BYTE : N�mero de Secuencia Inicial de la r�faga.
  //
  tpduMACK.N_SEC_INICIAL_RAFAGA = new SecuenceNumber (buffer.getInt (offset));
  offset+=4;


  return tpduMACK;
 }



 //===========================================================================























   * Devuelve el {@link #ID_SOCKET_FUENTE id_socket fuente}.
   * @return id_socket fuente
   */
 ClusterMemberID getID_SocketFuente ()












   * Devuelve el n�mero de r�faga fuente.
   */
  int getNumeroRafagaFuente ()






   * Devuelve el n�mero de secuencia inicial de la r�faga.
   * @return n�mero de secuencia inicial de la r�faga
   */
  SecuenceNumber getNumSecInicialRafaga ()

































