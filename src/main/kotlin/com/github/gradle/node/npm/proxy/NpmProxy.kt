package com.github.gradle.node.npm.proxy

import java.net.URLEncoder
import java.util.stream.Collectors.toList
import java.util.stream.Stream
import kotlin.text.Charsets.UTF_8

internal class NpmProxy {
    companion object {
        fun computeNpmProxyEnvironmentVariables(): Map<String, String> {
            val proxyEnvironmentVariables = computeProxyUrlEnvironmentVariables()
            if (proxyEnvironmentVariables.isNotEmpty()) {
                addProxyIgnoredHostsEnvironmentVariable(proxyEnvironmentVariables)
            }
            return proxyEnvironmentVariables.toMap()
        }

        private fun computeProxyUrlEnvironmentVariables(): MutableMap<String, String> {
            val proxyArgs = mutableMapOf<String, String>()
            for ((proxyProto, proxyParam) in
            listOf(arrayOf("http", "HTTP_PROXY"), arrayOf("https", "HTTPS_PROXY"))) {
                var proxyHost = System.getProperty("$proxyProto.proxyHost")
                val proxyPort = System.getProperty("$proxyProto.proxyPort")
                if (proxyHost != null && proxyPort != null) {
                    proxyHost = proxyHost.replace("^https?://".toRegex(), "")
                    val proxyUser = System.getProperty("$proxyProto.proxyUser")
                    val proxyPassword = System.getProperty("$proxyProto.proxyPassword")
                    if (proxyUser != null && proxyPassword != null) {
                        proxyArgs[proxyParam] =
                                "$proxyProto://${encode(proxyUser)}:${encode(proxyPassword)}@$proxyHost:$proxyPort"
                    } else {
                        proxyArgs[proxyParam] = "$proxyProto://$proxyHost:$proxyPort"
                    }
                }
            }
            return proxyArgs
        }

        private fun encode(value: String): String {
            return URLEncoder.encode(value, UTF_8.toString())
        }

        private fun addProxyIgnoredHostsEnvironmentVariable(proxyEnvironmentVariables: MutableMap<String, String>) {
            val proxyIgnoredHosts = computeProxyIgnoredHosts()
            if (proxyIgnoredHosts.isNotEmpty()) {
                proxyEnvironmentVariables["NO_PROXY"] = proxyIgnoredHosts.joinToString(", ")
            }
        }

        private fun computeProxyIgnoredHosts(): List<String> {
            return Stream.of("http.nonProxyHosts", "https.nonProxyHosts")
                    .map { property ->
                        val propertyValue = System.getProperty(property)
                        if (propertyValue != null) {
                            val hosts = propertyValue.split("|")
                            return@map hosts
                                    .map { host ->
                                        if (host.contains(":")) host.split(":")[0]
                                        else host
                                    }
                        }
                        return@map listOf<String>()
                    }
                    .flatMap(List<String>::stream)
                    .distinct()
                    .collect(toList())
        }
    }
}
