name := """dataplane-profiler"""

Common.settings

lazy val commons = (project in file("commons"))

lazy val profilerAgent = (project in file("profiler-agent")).enablePlugins(PlayScala)
  .dependsOn(commons)

lazy val hiveMetastoreProfiler = (project in file("profilers/hive-metastore-profiler")).dependsOn(commons)

lazy val hiveProfiler = (project in file("profilers/hive-profiler")).dependsOn(commons)

lazy val atlasCommons = (project in file("profilers/atlas-commons"))

lazy val geoProfiler = (project in file("profilers/geo-profiler")).dependsOn(atlasCommons)

lazy val auditProfiler = (project in file("profilers/audit-profiler"))

lazy val sensitiveInfoProfiler = (project in file("profilers/sensitive-info-profiler")).dependsOn(atlasCommons).dependsOn(commons)

lazy val tablestatsProfiler = (project in file("profilers/tablestats-profiler")).dependsOn(atlasCommons).dependsOn(commons)