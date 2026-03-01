package to.joeli.jass.client.strategy.training.networks

import org.slf4j.LoggerFactory
import org.tensorflow.SavedModelBundle
import org.tensorflow.ndarray.NdArrays
import org.tensorflow.ndarray.Shape
import org.tensorflow.types.TFloat32
import to.joeli.jass.client.strategy.helpers.ShellScriptRunner
import to.joeli.jass.client.strategy.training.NetworkType
import to.joeli.jass.client.strategy.training.data.DataSet


/**
 * Decision not to use DL4J anymore for the following reasons:
 * - Throws UnsupportedKerasConfigurationException: Unsupported keras layer type Softmax for special cards estimator network configuration
 * - Training is a pain in DL4J: No Tensorboard support, own visualization tool does not work, very hard to get metrics
 * - Documentation is horrible
 * --> Conclusion: Use Keras and ZeroMQ for fast communication between java and python
 *
 * New Decision:
 * Because ZeroMQ had problem "Too many open files"
 * Ditched ZeroMQ in favor of direct model import via tensorflow java api
 */
open class NeuralNetwork(private val networkType: NetworkType, var isTrainable: Boolean) {

    var savedModelBundle: SavedModelBundle? = null


    fun loadModel(episode: Int) {
        val path =  "${DataSet.getEpisodePath(episode)}${networkType.path}models/export/"
        savedModelBundle = SavedModelBundle.load(path, "tag")
    }

    @Synchronized
    fun predict(features: Array<FloatArray>): TFloat32 {
        if (savedModelBundle == null)
            throw IllegalStateException("There is no neural network loaded! Cannot make any predictions!")

        val rows = features.size
        val cols = features[0].size
        val inputTensor = TFloat32.tensorOf(Shape.of(1, rows.toLong(), cols.toLong()))
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                inputTensor.setFloat(features[r][c], 0, r.toLong(), c.toLong())
            }
        }

        val result = savedModelBundle!!.session().runner()
                .feed("input", inputTensor)
                .fetch(networkType.output)
                .run()
        return result[0] as TFloat32
    }

    companion object {
        /**
         * Trains the network with a given train mode. The actual training is done in python with keras. This is why we invoke the shell script.
         */
        @JvmStatic
        fun train(episode: Int, networkType: NetworkType): Boolean {
            return ShellScriptRunner.runShellProcess(ShellScriptRunner.pythonDirectory, "python3 train.py ${zeroPadded(episode)} ${networkType.path}")

        }

        private fun zeroPadded(number: Int): String {
            return String.format("%04d", number)
        }

        val logger = LoggerFactory.getLogger(NeuralNetwork::class.java)
    }
}
