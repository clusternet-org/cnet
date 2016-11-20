//============================================================================
//
//	Copyright (c) 1999-2015 . All Rights Reserved.
//
//----------------------------------------------------------------------------
//
//	Fichero: TPDU.java  1.0 9/9/99
//
//
//	Descripción: Clase TPDU.
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
//----------------------------------------------------------------------------

package cnet;

import java.io.*;
import java.util.*;


/**
/**
 * Clase TPDU. Los siguiente campos son comunes a todos los TPDU.<br>
 *
 * <br>
 *                      1 1 1 1 1 1 1 1 1 1 2 2 2 2 2 2 2 2 2 2 3 3<br>
 * +0-1-2-3-4-5-6-7-8-9-0-1-2-3-4-5-6-7-8-9-0-1-2-3-4-5-6-7-8-9-0-1<br>
 * +---------------------------------------------------------------+<br>
 * +      Puerto Multicast         |          Puerto Unicast       +<br>
 * +---------------------------------------------------------------+<br>
 * +                    ID_GRUPO_LOCAL(4 bytes primeros)           +<br>
 * +---------------------------------------------------------------+<br>
 * +ID_GRUPO_LOCAL(2 bytes últimos)|          Longitud             +<br>
 * +---------------------------------------------------------------+<br>
 * +           Cheksum             + V |ti-|
 * +                               +   |po |
 * +----------------------------------------
 *
 * <br>
 * Esta clase no es thread-safe.<br>
 * Subclases:
 * <ul>
 *  <li>{@link TPDUDatos}</li>
 *  <li>{@link TPDUCGL}</li>
 * </ul>
 * @version  1.0
 * @author M. Alejandro García Domínguez
 * <A HREF="mailto:alejandro.garcia.dominguez@gmail.com">(alejandro.garcia.dominguez@gmail.com)</A><p>
 * Antonio Berrocal Piris
 * <A HREF="mailto:AntonioBP.wanadoo.es">(AntonioBP@wanadoo.es)</A><p>
 */

class TPDU implements Cloneable
{

  /** Longitud de la cabecera (en bytes) */
  static final int LONGHEADER = 3*4 + 3; // 15 bytes

  /** Puerto Multicast (16 bits) */
  protected int PUERTO_MULTICAST = 0;

  /** Puerto Unicast (16 bits) */
  protected int PUERTO_UNICAST = 0;

  /** Longitud del TPDU (16 bits) */
  protected int LONGITUD = 0;

  /** Cheksum (16 bits) */
  protected int CHEKSUM = 0;

  /** Versión (2 bits): Versión del protocolo ClusterNet */
  protected byte VERSION = ClusterNet.VERSION;

  /** Codificado (1 bit): Si está a 1 indica que el TPDU está  */
  protected byte CODIFICADO = 0;

  /**
   * ID_grupo_local (48 bits):  Identificativo del grupo local. Cada grupo
   * local tiene que tener un nº identificativo único e independiente de cualquier
   * otro grupo local.
   */
  protected ClusterGroupID ID_GRUPO_LOCAL = null;

  /** Tipo (2 bits): 01 (TPDU DATOS) */
  protected byte TIPO  = ClusterNet.TPDU_CGL;

  /** Objeto ClusterNet */
  protected SocketClusterNetImp socketClusterNetImp= null;



  //==========================================================================
  /**
   * Constructor por defecto.
   * Este constructor es para crear TPDUS a partir del parser de un Buffer
   * @exception ClusterNetExcepcion
   */
  protected TPDU() throws ClusterNetExcepcion
  {
    super();
  }

  //==========================================================================
  /**
   * Constructor utilizado para crear un TPDU, obtiene la información de los
   * campos a partir del objeto socketClusterNetImp.
   * @param socketClusterNetImp Objeto SocketClusterNetImp del que obtiene el valor de los
   * campos de la cabecera común.
   * @exception ClusterNetExcepcion
   * @exception ClusterNetInvalidParameterException lanzada si socketClusterNetImp es null
   */
  protected TPDU(SocketClusterNetImp socketClusterNetImp)
    throws ClusterNetInvalidParameterException, ClusterNetExcepcion
  {
    super();

    final String  mn = "TPDU.TPDU(socketClusterNetImp,int)";

    this.socketClusterNetImp = socketClusterNetImp;

    // TPDU
    this.PUERTO_MULTICAST = this.socketClusterNetImp.getCanalMcast().getAddressMulticast().getPort();
    this.PUERTO_UNICAST   = this.socketClusterNetImp.getCanalUnicast().getAddressUnicast().getPort();
    this.ID_GRUPO_LOCAL   = this.socketClusterNetImp.getIDGL();
    this.LONGITUD = 0;
    this.CHEKSUM = 0;
    this.VERSION = ClusterNet.VERSION;
  }

  //==========================================================================
  /**
   * Constructor utilizado para crear un TPDU a partir de la información
   * facilitada en los argumentos.
   * @param puertoMulticast
   * @param puertoUnicast
   * @param clusterGroupID
   * @exception ClusterNetExcepcion
   * @exception ClusterNetInvalidParameterException lanzada si algún parámetro tienen un
   * valor no válido.
   */
  protected TPDU(int puertoMulticast,int puertoUnicast,ClusterGroupID clusterGroupID)
                             throws  ClusterNetInvalidParameterException, ClusterNetExcepcion
  {
    super();

    final String  mn = "TPDU.TPDU(puertoMcast,puertoUnicast,clusterGroupID,tamañoTotal)";


    // TPDU
    this.PUERTO_MULTICAST = puertoMulticast;
    this.PUERTO_UNICAST   = puertoUnicast;
    this.ID_GRUPO_LOCAL   = clusterGroupID;
    this.LONGITUD = 0;
    this.CHEKSUM = 0;
    this.VERSION = ClusterNet.VERSION;

  }

  //==========================================================================
  /**
   * Método clone del TPDU.
   * @return El nuevo objeto clonado.
   */
  protected Object clone()
  {
    final String  mn = "TPDU.clone()";
    TPDU pkt = null;

    //
    // Clonar el TPDU, después clonar el buffer.
    //
    pkt.PUERTO_MULTICAST = this.PUERTO_MULTICAST;

    pkt.PUERTO_UNICAST = this.PUERTO_UNICAST;

    pkt.LONGITUD = this.LONGITUD;

    pkt.CHEKSUM = this.CHEKSUM;

    pkt.VERSION = this.VERSION;

    pkt.ID_GRUPO_LOCAL = this.ID_GRUPO_LOCAL;

    pkt.TIPO  = this.TIPO;

    pkt.socketClusterNetImp = this.socketClusterNetImp;

    return(pkt);
  }

  //==========================================================================
  /**
   * El buffer pasado por argumento contiene un TPDU recibido de la red.
   * Esta función extrae el valor del campo puerto multicast.
   * @param buf buffer que contiene un TPDU.
   * @return puerto multicast
   * @exception ClusterNetExcepcion lanzada si hubo un error al leer el buffer
   */
  static int getPuertoMulticast(Buffer buf)
   throws ClusterNetExcepcion
  {
    final String  mn = "TPDU.getPuertoMulticast";
    int puerto =0;

    if ( buf.getLength() < 2)
    {
      throw new ClusterNetExcepcion(mn, "Buffer demasiado pequeño.");
    }

    try
    {
      //
      // 1º y 2º BYTE :Puerto Multicast (16 bits).
      //
      puerto = buf.getShort(0);
    }
    catch(ClusterNetInvalidParameterException e)
    {
      throw new ClusterNetExcepcion(e.getMessage());
    }

    return puerto;
  }

  //==========================================================================
  /**
   * El buffer pasado por argumento contiene un TPDU recibido de la red.
   * Esta función extrae el valor del compo longitud.
   * @param buf buffer que contiene un TPDU.
   * @return longitud
   * @exception ClusterNetExcepcion lanzada si hubo un error al leer el buffer
   */
  static int getLongitud(Buffer buf)
   throws ClusterNetExcepcion
  {
    final String  mn = "TPDU.getLongitud";
    int longitud = 0;

    if ( buf.getLength() < 2)
    {
      throw new ClusterNetExcepcion(mn, "Buffer demasiado pequeño.");
    }

    try
    {
      //
      // 11º y 12º BYTE :Longitud (16 bits).
      //
      longitud = buf.getShort(10);

    }
    catch(ClusterNetInvalidParameterException e)
    {
      throw new ClusterNetExcepcion(e.getMessage());
    }

    return longitud;
  }

  //==========================================================================
  /**
   * El buffer pasado por argumento contiene un TPDU recibido de la red.
   * Esta función extrae el valor del compo Versión.
   * @param buf buffer que contiene un TPDU.
   * @return versión
   * @exception ClusterNetExcepcion lanzada si hubo un error al leer el buffer
   */
  static int getVersion(Buffer buf)
   throws ClusterNetExcepcion
  {
    final String  mn = "TPDU.getVersion";
    int version = 0;

    if ( buf.getLength() < 2)
    {
      throw new ClusterNetExcepcion(mn, "Buffer demasiado pequeño.");
    }

    try
    {
      //
      // 15º BYTE : VERSION (2 bits), TIPO (2 bits), SUBTIPO (4 bits).
      //
      int offset = 14;
      version = (byte)((buf.getByte(offset) & 0xC0) >>> 6);
      offset++;

    }
    catch(ClusterNetInvalidParameterException e)
    {
      throw new ClusterNetExcepcion(e.getMessage());
    }

    if (version != ClusterNet.VERSION)
      throw new ClusterNetExcepcion(mn, "TPDU (paquete) recibida con versión ClusterNet incorrecta ("+version+")");

    return version;
  }


  //==========================================================================
  /**
   * El buffer pasado por argumento contiene un TPDU recibido de la red.
   * Esta función extrae el valor del compo Cheksum.
   * @param buf buffer que contiene un TPDU.
   * @return checksum
   * @exception ClusterNetExcepcion lanzada si hubo un error al leer el buffer
   */
  static int getCheksum(Buffer buf)
   throws ClusterNetExcepcion
  {
    final String  mn = "TPDU.getLongitud";
    int cheksum = 0;

    if ( buf.getLength() < 2)
    {
      throw new ClusterNetExcepcion(mn, "Buffer demasiado pequeño.");
    }

    try
    {
      //
      // 13º y 14º BYTE : Cheksum (16 bits).
      //
      cheksum = buf.getShort(12);

    }
    catch(ClusterNetInvalidParameterException e)
    {
      throw new ClusterNetExcepcion(e.getMessage());
    }

    return cheksum;
  }

  //==========================================================================
  /**
   * El buffer pasado por argumento contiene un TPDU recibido de la red.
   * Esta función extrae el valor del compo tipo.
   * @param buf buffer que contiene un TPDU.
   * @return tipo
   * @exception ClusterNetExcepcion lanzada si hubo un error al leer el buffer
   */
  static byte getTipo(Buffer buf) throws ClusterNetExcepcion
  {
   final String mn ="TPDU.getTipo";

   int  offset  = 14;

   byte tipo    = 0;

   //
   // Asegurarse que se ha recibido algún dato,
   // almenos los bytes de la cabecera del TPDU.
   //
   if (buf.getLength() < TPDU.LONGHEADER)
      throw new ClusterNetExcepcion(mn, "La cabecera del TPDU (paquete) recibido no se ha recibido entera. No se puede procesar el TPDU.");

   try
   {
      //
      // 15º BYTE : VERSION (2 bits), TIPO (2 bits), SUBTIPO (4 bits).
      //

      tipo = (byte)((buf.getByte(offset) & 0x30) >>> 4);
   }
   catch(ClusterNetInvalidParameterException e)
   {
      throw new ClusterNetExcepcion(mn,e.getMessage());
   }


   return tipo;
  }


  //==========================================================================
  /**
   * El buffer pasado por argumento contiene un TPDU recibido de la red.
   * Esta función extrae el valor del compo puerto unicast.
   * @param buf buffer que contiene un TPDU.
   * @return puerto unicast
   * @exception ClusterNetExcepcion lanzada si hubo un error al leer el buffer
   */
  static int getPuertoUnicast(Buffer buf) throws ClusterNetExcepcion
  {
   final String mn ="TPDU.getPuertoUnicast";


   int id_socket = 0;


   //
   // Asegurarse que se ha recibido algún dato,
   // almenos los  bytes de la cabecera del TPDU.
   //
   if (buf.getLength() < TPDU.LONGHEADER)
      throw new ClusterNetExcepcion(mn, "La cabecera del TPDU (paquete) recibido no se ha recibido entera. No se puede procesar el TPDU.");

   try
   {
      id_socket = (buf.getShort(2)) ;

   }
   catch(ClusterNetInvalidParameterException e)
   {
      throw new ClusterNetExcepcion(mn,e.getMessage());
   }


   return id_socket;
  }

  //==========================================================================
  /**
   * El buffer pasado por argumento contiene un TPDU recibido de la red.
   * Esta función extrae el valor del compo ClusterGroupID.
   * @param buf buffer que contiene un TPDU.
   * @return clusterGroupID
   * @exception ClusterNetExcepcion lanzada si hubo un error al leer el buffer
   */
  static ClusterGroupID getIDGL(Buffer buf) throws ClusterNetExcepcion
  {
   final String mn ="TPDU.getIDGrupoLocal";


   ClusterGroupID clusterGroupID = null;



   //
   // Asegurarse que se ha recibido algún dato,
   // almenos los  bytes de la cabecera del TPDU.
   //
   if (buf.getLength() < TPDU.LONGHEADER)
      throw new ClusterNetExcepcion(mn, "La cabecera del TPDU (paquete) recibido no se ha recibido entera. No se puede procesar el TPDU.");

   try
   {
      //
      // 5º, 6º, 7º, 8º, 9º y 10º BYTES: ID_GRUPO_LOCAL (48 bits)
      //
      clusterGroupID = new ClusterGroupID (new Buffer(buf.getBytes(4,(byte) 6)),(byte)0);
   }
   catch(ClusterNetInvalidParameterException e)
   {
      throw new ClusterNetExcepcion(mn,e.getMessage());
   }


   return clusterGroupID;
  }

  //==========================================================================
  /**
   *  Devuelve el Puerto Multicast de este TPDU.
   */
   int getPuertoMulticast() {  return this.PUERTO_MULTICAST; }

  //==========================================================================
  /**
   *  Devuelve el Puerto Unicast de este TPDU.
   */
   int getPuertoUnicast() {  return this.PUERTO_UNICAST; }

  //==========================================================================
  /**
   *  Devuelve el tipo de este TPDU.
   */
   byte getTipo() { return this.TIPO;  }

   //==========================================================================
   /**
   * Devuelve el ClusterGroupID de este TPDU.
   */
   ClusterGroupID getIDGL() { return this.ID_GRUPO_LOCAL;  }

  //==========================================================================
  /**
   *  Devuelve la versión de este TPDU.
   */
   byte getVersion() { return this.VERSION; }


  //==========================================================================
  /**
   *  Devuelve la longitud de este TPDU.
   */
   int getLongitud() {  return this.LONGITUD;  }

  //==========================================================================
  /**
   *  Devuelve el Cheksum de este TPDU.
   */
   int getCheksum() { return this.CHEKSUM; }

  //==========================================================================
  /**
   * El buffer pasado tiene que contener un TPDU. Esta función extrae los
   * valores de la cabecera común a todos los TPDU:
   * <ul>
   *   <li>Puerto Multicast</li>
   *   <li>Puerto Unicast</li>
   *   <li>ClusterGroupID</li>
   *   <li>Longiud</li>
   *   <li>Cheksum</li>
   *   <li>Versión</li>
   *   <li>Tipo</li>
   * </ul><br>
   * Asigna al tpdu pasado por argumento los valores extraidos.
   * @param buf Objeto Buffer que contiene un TPDU
   * @param tpdu tpdu al que se asignan los valores de los campos de la cabecera
   * común extraidos desde el buffer.
   * @exception ClusterNetExcepcion Se lanza si ocurre un error al extraer los campos
   * del buffer.
   */
  static void parseCabeceraComun(Buffer buf,TPDU tpdu) throws ClusterNetExcepcion
  {
     final String mn = "TPDU.parseCabeceraComun";

     tpdu.PUERTO_MULTICAST = TPDU.getPuertoMulticast(buf);

     tpdu.PUERTO_UNICAST = TPDU.getPuertoUnicast(buf);

     tpdu.ID_GRUPO_LOCAL = TPDU.getIDGL(buf);

     tpdu.LONGITUD = TPDU.getLongitud(buf);

     tpdu.CHEKSUM = TPDU.getCheksum(buf);

     tpdu.VERSION  = (byte) TPDU.getVersion(buf);

     tpdu.TIPO = TPDU.getTipo(buf);

  }

  //==========================================================================
  /**
   * Crea un buffer del tamaño indicado y le introduce los datos de la cabecera
   * común.
   * @param Tipo valor del campo tipo
   * @exception ClusterNetExcepcion Es lanzada cuando ocurre algún error.
   * @exception ClusterNetInvalidParameterException si el tamaño del buffer a crear no
   * es suficiente para introducir los campos de la cabecera común, o si el tipo
   * no es válido.
   */
  Buffer construirCabeceraComun(short Tipo,int tamañoBuffer) throws ClusterNetExcepcion,
                                                ClusterNetInvalidParameterException
  {
    final String mn = "TPDU.construirCabeceraComun";
    int offset =0;
    Buffer bufferResult = null;


    // Introducir los datos en el buffer.
    this.TIPO = (byte)Tipo;

    // Comprobar si hay tamaño suficiente.
    if (tamañoBuffer < TPDU.LONGHEADER)
        throw new ClusterNetInvalidParameterException ("Tamaño no válido");

    // Contruir el buffer
    bufferResult = new Buffer (tamañoBuffer);

    //
    // 1º y 2º BYTE : Puerto Multicast (16 bits).
    //
    bufferResult.addShort(this.PUERTO_MULTICAST,offset);
    offset+=2;

    //
    // 3º y 4º BYTE : Puerto Unicast (16 bits).
    //
    bufferResult.addShort(this.PUERTO_UNICAST,offset);
    offset+=2;

    //
    // 5º, 6º, 7º, 8º, 9º y 10º BYTES: ID_GRUPO_LOCAL (48 bits)
    //
    if ( ( ID_GRUPO_LOCAL == null)
         || ( ID_GRUPO_LOCAL.id.getMaxLength() != 6))
      {
        bufferResult.addInt(0,offset);
        bufferResult.addShort(0,offset+4);
      }
    else
      {
        bufferResult.addBytes( this.ID_GRUPO_LOCAL.id,0,offset/*offset*/,6 /*longitud*/);
      }
    offset+=6;

    //
    // 11º y 12º BYTE : Longitud (16 bits).
    //
    bufferResult.addShort(this.LONGITUD,offset);
    offset+=2;

    //
    // 13º y 14º BYTE : Cheksum (16 bits).
    //
    bufferResult.addShort(this.CHEKSUM,offset);
    offset+=2;

    //
    // 15º BYTE : VERSION (2 bits), TIPO (2 bits).
    //
    bufferResult.addByte((byte)
      ((((ClusterNet.VERSION << 6) & 0xC0) |
      ((TIPO   << 4) & 0x30) )),offset);
    offset+=1;


    return bufferResult;
  }
}
