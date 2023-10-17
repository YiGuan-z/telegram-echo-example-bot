package module.opencv

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import module.thisLogger
import org.bytedeco.javacpp.Loader
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import kotlin.io.path.Path
import kotlin.io.path.pathString
import org.bytedeco.opencv.opencv_java

/**
 *
 * @author caseycheng
 * @date 2023/10/16-17:57
 * @doc
 **/
object OpenCVService {
    fun init() {
        Loader.load(opencv_java::class.java)
    }

    private val logger = thisLogger<OpenCVService>()
    suspend fun bgr2rgb(path: String): Mat {
        return conversion(path, Imgproc.COLOR_BGR2RGB)
    }

    suspend fun bgr2gray(path: String): Mat {
        return conversion(path, Imgproc.COLOR_BGR2GRAY)
    }

    suspend fun conversionImageFile(
        path: String,
        to: String,
        width: Double,
        height: Double,
        scale: Double
    ) {
        val path = path.drop(2)
        val to = to.drop(2)
        withContext(Dispatchers.IO) {
            val path = Path(path).toAbsolutePath().pathString
            val to = Path(to).toAbsolutePath().pathString
            logger.info("[openCV] conversionImageFile $path $to")
            //TODO 这里如果格式不支持的话，就会为空，所以需要判断空
            val src = Imgcodecs.imread(path)
            val dst = Mat()
            val size = Size(width * scale, height * scale)
            withContext(Dispatchers.Default) {
                if (src.empty()){
                    logger.info("[openCV] conversionImageFile $path is empty")
                    throw RuntimeException()
                }else{
                    logger.info("[openCV] conversionImageFile $path is not empty")
                }
                Imgproc.resize(src, dst, size)
            }
            withContext(Dispatchers.IO) {
                Imgcodecs.imwrite(to, dst)
            }
        }
    }

    suspend fun conversion(path: String, code: Int): Mat {
        val image: Mat
        val result = Mat()
        withContext(Dispatchers.IO) {
            image = Imgcodecs.imread(path)
        }
        withContext(Dispatchers.Default) {
            Imgproc.cvtColor(image, result, code)
        }
        return result
    }


}