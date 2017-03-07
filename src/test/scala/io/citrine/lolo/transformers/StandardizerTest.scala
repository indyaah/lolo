package io.citrine.lolo.transformers

import io.citrine.lolo.TestUtils
import io.citrine.lolo.linear.{GuessTheMeanLearner, LinearRegressionLearner}
import io.citrine.lolo.stats.functions.Friedman
import org.junit.Test

/**
  * Created by maxhutch on 2/19/17.
  */
@Test
class StandardizerTest {

  val data = TestUtils.generateTrainingData(1024, 12, noise = 0.1, function = Friedman.friedmanSilverman)

  @Test
  def testStandardMeanAndVariance(): Unit = {
    val inputs = data.map(_._1)
    val inputTransforms = Standardizer.getMultiStandardization(inputs)

    val standardInputs = Standardizer.applyStandardization(inputs, inputTransforms)
    standardInputs.head.indices.foreach{ i =>
      val values = standardInputs.map(_(i).asInstanceOf[Double])
      val mean = values.sum / values.size
      assert(mean < 1.0e-9, s"Standard input ${i} has non-zero mean ${mean}")

      val variance = values.map(Math.pow(_, 2)).sum / values.size
      assert(Math.abs(variance - 1.0) < 1.0e-9, s"Standard input ${i} has non-unit variance ${variance}")
    }

    val outputs = data.map(_._2.asInstanceOf[Double])
    val outputTransform = Standardizer.getStandardization(outputs)
    val standardOutputs = Standardizer.applyStandardization(outputs, Some(outputTransform)).asInstanceOf[Seq[Double]]

    val mean = standardOutputs.sum / standardOutputs.size
    assert(mean < 1.0e-9, s"Standard output has non-zero mean ${mean}")

    val variance = standardOutputs.map(Math.pow(_, 2)).sum / standardOutputs.size
    assert(Math.abs(variance - 1.0) < 1.0e-9, s"Standard output has non-unit variance ${variance}")
  }

  /**
    * Guess the mean should be invariant under standardization
    */
  @Test
  def testStandardGTM(): Unit = {
    val learner = new GuessTheMeanLearner
    val model = learner.train(data).getModel()
    val result = model.transform(data.map(_._1)).getExpected()

    val standardLearner = new Standardizer(new GuessTheMeanLearner)
    val standardModel = standardLearner.train(data).getModel()
    val standardResult = standardModel.transform(data.map(_._1)).getExpected()

    result.zip(standardResult).foreach{ case (free: Double, standard: Double) =>
        assert(Math.abs(free - standard) < 1.0e-9, s"${free} and ${standard} should be the same")
    }
  }

  /**
    * Linear regression should be invariant under standardization
    */
  @Test
  def testStandardLinear(): Unit = {
    val learner = new LinearRegressionLearner()
    val model = learner.train(data).getModel()
    val result = model.transform(data.map(_._1)).getExpected()

    val standardLearner = new Standardizer(learner)
    val standardModel = standardLearner.train(data).getModel()
    val standardResult = standardModel.transform(data.map(_._1)).getExpected()

    result.zip(standardResult).foreach{ case (free: Double, standard: Double) =>
        assert(Math.abs(free - standard) < 1.0e-9, s"${free} and ${standard} should be the same")
    }
  }

  /**
    * Ridge regression should depend on standardization
    */
  @Test
  def testStandardRidge(): Unit = {
    val learner = new LinearRegressionLearner().setHyper("regParam", 1.0)
    val model = learner.train(data).getModel()
    val result = model.transform(data.map(_._1)).getExpected()

    val standardLearner = new Standardizer(learner)
    val standardModel = standardLearner.train(data).getModel()
    val standardResult = standardModel.transform(data.map(_._1)).getExpected()

    result.zip(standardResult).foreach{ case (free: Double, standard: Double) =>
        assert(Math.abs(free - standard) > 1.0e-9, s"${free} and ${standard} should NOT be the same")
    }
  }
}

object StandardizerTest {
  def main(argv: Array[String]): Unit = {
    new StandardizerTest().testStandardMeanAndVariance()
    new StandardizerTest().testStandardGTM()
    new StandardizerTest().testStandardLinear()
    new StandardizerTest().testStandardRidge()
  }
}
