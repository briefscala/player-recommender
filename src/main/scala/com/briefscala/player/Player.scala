package com.briefscala.player

import org.apache.spark.mllib.recommendation.{ALS, Rating}
import org.apache.spark.{SparkContext, SparkConf}

import scala.util.Try

object Player {
  def main(args: Array[String]): Unit = {
    val sconf = new SparkConf()
      .setAppName("Player recommender")
      .set("spark.executor.memory", "6g")
      .set("spark.eventLog.enabled", "true")
      .set("spark.eventLog.dir", "/log/spark")

    val sc = new SparkContext(sconf)

    /**
      * the directory where the data resides
      */
    val loadingDir = "src/main/resources"

    /**
      * ask spark to create a minimum amount of partitions
      * this can help if collecting the data is desired and
      * you want to make sure the partion fits in the driver
      */
    val rawUserArtistData = sc.textFile(s"$loadingDir/user_artist_data.txt", 10)
    val rawArtistData = sc.textFile(s"$loadingDir/artist_data.txt", 10)
    val rawArtistAlias = sc.textFile(s"$loadingDir/artist_alias.txt", 10)

    /**
      * our raw data is likely to contain bad data so we
      * will discard the any line that won't parse successfully
      *
      * discard unparsable id toInt
      */
    val artistByID = rawArtistData.flatMap { line =>
      val (id, name) = line.span(_ != '\t')
      Try((id.toInt, name.trim)).toOption
    }

    /**
      * discard tokens that won't parse toInt
      */
    val artistAlias = rawArtistAlias.flatMap { line =>
      val (token0, token1) = line.span(_ != '\t')
      Try((token0.toInt, token1.toInt)).toOption
    }.collectAsMap()

    /**
      * artist aliases are a small data set and can efficiently
      * be stored in each node for retrieving the artistID
      */
    val bAliases = sc.broadcast(artistAlias)

    /**
      * rawUserArtistData need to be parsed and converted to Ratings
      * We will use the number of times an artist was played as rating
      */
    val data = rawUserArtistData.flatMap { line =>
      val Array(tryUserID, tryArtistID, tryCount) =
        line.split(' ').map(part => Try(part.toInt))
      val tryRatings = for {
        userID   <- tryUserID
        artistID <- tryArtistID
        count    <- tryCount
        finalArtistID = bAliases.value.getOrElse(artistID, artistID)
      } yield Rating(userID, finalArtistID, count)
      tryRatings.toOption
    }.cache()

    /**
      * Collaborative Filtering for Implicit Feedback Datasets:
      * Train a matrix factorization model given an RDD of 'implicit preferences' ratings
      * (artist played count works as rating per artist here for us) given by users to some
      * artist, in the form of (userID, artistID, rating(played count)) tuple3.
      * We approximate the ratings matrix as the product of two lower-rank matrices of a given
      * rank (number of features). To solve for these features, we run a given number of iterations
      * of ALS. The level of parallelism is determined automatically based on the number of partitions
      * in ratings. Model parameters alpha and lambda are set to reasonable default values
      *
      * rank: number of features to use
      * iterations: number of iterations of ALS (recommended: 10-20)
      * lambda: regularization factor (recommended: 0.01)
      */
    val model = ALS.trainImplicit(data, rank = 10, iterations = 20, lambda = 0.01, alpha = 0.95)

    /**
      * the model can then be query to get artists recommendations of a given user
      * as a spot check we can choose a random userID and print 5 recommended artists
      */
    val recommendations = model.recommendProducts(2203500, 5)
    val recommendedProductIDs = recommendations.map(_.product).toSet
    val allRecommendations = artistByID.filter { case (id, name) =>
      recommendedProductIDs.contains(id)
    }.values
    allRecommendations
      .collect()
      .foreach(println)
    sc.stop()
  }
}
