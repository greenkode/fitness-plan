package com.krachtix.aws.integration

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class AwsConfig(val s3ConfigProperties: AwsS3ConfigProperties) {

    @Bean
    fun getAmazonS3Client(): AmazonS3 {
        return AmazonS3ClientBuilder.standard()
            .withCredentials(
                AWSStaticCredentialsProvider(
                    BasicAWSCredentials(
                        s3ConfigProperties.accessKey, s3ConfigProperties.secret
                    )
                )
            )
            .withEndpointConfiguration(
                AwsClientBuilder.EndpointConfiguration(
                    s3ConfigProperties.endpoint, s3ConfigProperties.region
                )
            )
            .withPathStyleAccessEnabled(true)
            .build()
    }

    @ConfigurationProperties(prefix = "application.files.storage-provider.aws-s3")
    class AwsS3ConfigProperties(
        val accessKey: String,
        val secret: String,
        val region: String,
        var endpoint: String,
        val bucket: String = "krachtix-exports"
    )
}