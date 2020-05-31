package immutabledb

import immutabledb.codec._
import immutabledb.storage._

object Loader {
    def run = {
        main(Array("/Users/marcin/projects/immutable3/data_10m.csv", "data_10m"))
    }

    def main(args: Array[String]) = {
        val csvFilePath = args(0)
        val outDir = args(1)

        val bufferedSource = io.Source.fromFile(csvFilePath)

        val lines = bufferedSource.getLines()
        val first = lines.next().split(",").toList

        val colId = Column.make("id", CodecType.DENSE_INT)
        val colState = Column.make("state", CodecType.DENSE_STRING, Map("size" -> "2"))
        val colAge = Column.make("age", CodecType.DENSE_TINYINT)

        val table = Table(
            outDir,
            List(colId, colState, colAge),
            DevEnv.config.blockSize)

        TableIO.store(DevEnv.config.dataDir, table)

        var segId = new SegmentWriter(0, table.blockSize, table.name, colId)(DevEnv)
        var segState = new SegmentWriter(0, table.blockSize, table.name, colState)(DevEnv)
        var segAge = new SegmentWriter(0, table.blockSize, table.name, colAge)(DevEnv)

        for (line <- lines) {
            val cols = line.split(",").map(_.trim)

            if (segId.remaining > 0) {
                segId.write(cols(0))
            } else {
                segId.close()
                segId = segId.newSegment()
                segId.write(cols(0))
            }

            if (segState.remaining > 0) {
                segState.write(cols(1))
            } else {
                segState.close()
                segState = segState.newSegment()
                segState.write(cols(1))
            }

            if (segAge.remaining > 0) {
                segAge.write(cols(2))
            } else {
                segAge.close()
                segAge = segAge.newSegment()
                segAge.write(cols(2))
            }
        }
        segId.close()
        segState.close()
        segAge.close()
    }
}