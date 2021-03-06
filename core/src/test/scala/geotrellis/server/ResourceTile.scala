package geotrellis.server

import geotrellis.server.vlm._
import geotrellis.contrib.vlm.gdal._
import geotrellis.raster._
import geotrellis.raster.io.geotiff.AutoHigherResolution
import geotrellis.contrib.vlm.TargetRegion
import geotrellis.raster.resample.NearestNeighbor
import geotrellis.vector.Extent

import com.azavea.maml.ast.{Literal, MamlKind, RasterLit}
import cats.effect._
import cats.data.{NonEmptyList => NEL}

case class ResourceTile(name: String) {
  def uri = {
    val f = getClass.getResource(s"/$name").getFile
    s"file://$f"
  }
}

object ResourceTile extends RasterSourceUtils {
  def getRasterSource(uri: String): GDALBaseRasterSource = GDALRasterSource(uri)

  implicit val extentReification: ExtentReification[ResourceTile] = new ExtentReification[ResourceTile] {
    def kind(self: ResourceTile): MamlKind = MamlKind.Image
    def extentReification(self: ResourceTile)(implicit contextShift: ContextShift[IO]): (Extent, CellSize) => IO[Literal] =
      (extent: Extent, cs: CellSize) => {
        getRasterSource(self.uri.toString)
          .resample(TargetRegion(RasterExtent(extent, cs)), NearestNeighbor, AutoHigherResolution)
          .read(extent)
          .map { RasterLit(_) }
          .toIO { new Exception(s"No tile avail for RasterExtent: ${RasterExtent(extent, cs)}") }
      }
  }

  implicit val nodeRasterExtents: HasRasterExtents[ResourceTile] = new HasRasterExtents[ResourceTile] {
    def rasterExtents(self: ResourceTile)(implicit contextShift: ContextShift[IO]): IO[NEL[RasterExtent]] =
      getRasterExtents(self.uri.toString)
  }

}


