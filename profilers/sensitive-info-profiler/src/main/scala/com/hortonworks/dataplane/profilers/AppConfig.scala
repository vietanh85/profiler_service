/*
 *   HORTONWORKS DATAPLANE SERVICE AND ITS CONSTITUENT SERVICES
 *
 *   (c) 2016-2018 Hortonworks, Inc. All rights reserved.
 *
 *   This code is provided to you pursuant to your written agreement with Hortonworks, which may be the terms of the
 *   Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 *   to distribute this code.  If you do not have a written agreement with Hortonworks or with an authorized and
 *   properly licensed third party, you do not have any rights to this code.
 *
 *   If this code is provided to you under the terms of the AGPLv3:
 *   (A) HORTONWORKS PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;
 *   (B) HORTONWORKS DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS CODE, INCLUDING BUT NOT
 *     LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;
 *   (C) HORTONWORKS IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS FOR ANY CLAIMS ARISING
 *     FROM OR RELATED TO THE CODE; AND
 *   (D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, HORTONWORKS IS NOT LIABLE FOR ANY
 *     DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES INCLUDING, BUT NOT LIMITED TO,
 *     DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF INCOME, LOSS OF BUSINESS ADVANTAGE OR UNAVAILABILITY,
 *     OR LOSS OR CORRUPTION OF DATA.
 */

package com.hortonworks.dataplane.profilers

import com.hortonworks.dataplane.profilers.commons.Models.{AtlasInfo, InputArgs, JobInput}
import com.hortonworks.dataplane.profilers.commons.parse.{KerberosCredentials, ParseKerberosCredentials}
import com.typesafe.scalalogging.LazyLogging
import org.apache.hadoop.fs.FileSystem
import org.apache.spark.sql.SparkSession
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse

class AppConfig(args: Seq[String]) extends LazyLogging {
  val pathPartitionedSuffix = "/hivesensitivitypartitioned/snapshot"
  val pathSuffix = "/hivesensitivity/snapshot"
  val input: InputArgs = parseInput(args.head)

  logger.debug("Args : {}", args.head)
  logger.debug("Input : {}", input.toString)

  val securityCredentials: KerberosCredentials = input.kerberosCredentials

  val sparkSession: SparkSession = securityCredentials.isSecured match {
    case "true" =>
      SparkSession.builder().appName("SensitiveInfoProfiler")
        .config("hive.metastore.uris", input.metastoreUrl).
        config("hive.metastore.kerberos.keytab", securityCredentials.keytab).
        config("hive.metastore.kerberos.principal", securityCredentials.principal).
        config("hive.metastore.sasl.enabled", true).
        enableHiveSupport().getOrCreate()
    case _ =>
      SparkSession.builder().appName("SensitiveInfoProfiler")
        .config("hive.metastore.uris", input.metastoreUrl)
        .enableHiveSupport().getOrCreate()
  }

  val hdfsFileSystem: FileSystem = FileSystem.get(sparkSession.sparkContext.hadoopConfiguration)


  private def parseInput(json: String): InputArgs = {
    implicit val formats: DefaultFormats.type = DefaultFormats
    val input = parse(json)
    val sampleSize = (input \ "jobconf" \ "sampleSize").extract[String].toLong
    val path = (input \ "clusterconfigs" \ "assetMetricsPath").extract[String]
    val metastoreUrl = (input \ "clusterconfigs" \ "metastoreUrl").extract[String]

    val kerberosCredentials = ParseKerberosCredentials.getkerberosCredentials(input)

    val tables = (input \\ "assets").children.map {
      asset =>
        asset.extract[JobInput]
    }


    val url = (input \ "clusterconfigs" \ "atlasUrl").extract[String]
    val user = (input \ "clusterconfigs" \ "atlasUser").extract[String]
    val password = (input \ "clusterconfigs" \ "atlasPassword").extract[String]
    val clusterName = (input \ "clusterconfigs" \ "clusterName").extract[String]
    val atlasInfo = AtlasInfo(url, user, password, clusterName)
    InputArgs(metastoreUrl, sampleSize, path, atlasInfo, tables, kerberosCredentials)

  }
}
