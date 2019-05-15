package oscar.cbls.business.geometry.visu

import java.awt.Color

import org.locationtech.jts.geom.Geometry
import oscar.cbls.visual.SingleFrameWindow
import oscar.cbls.visual.geometry.{GeometryDrawing, GeometryDrawingOnRealMap, GeometryDrawingTypes}

class Drawing (title: String,
               relevantDistances: List[(Int,Int)],
               geometryDrawingType: GeometryDrawingTypes.Value,
               area: Option[List[(Int,Int)]] = None,
               pointOfOrigin: Option[(Double, Double)] = None) {
  val (drawing, background) = geometryDrawingType match {
    case GeometryDrawingTypes.Simple =>
      (GeometryDrawing(relevantDistances,geometryDrawingType,area,pointOfOrigin), None)
    case GeometryDrawingTypes.OnRealMap =>{
      val background = GeometryDrawing(relevantDistances,GeometryDrawingTypes.OnRealMap,area,pointOfOrigin).asInstanceOf[GeometryDrawingOnRealMap]
      (GeometryDrawing(relevantDistances,GeometryDrawingTypes.Simple,area,pointOfOrigin,pointShift = Some(() => background.topLeftPointShiftInPixel)), Some(background))
    }
  }
  SingleFrameWindow.show(drawing, title, backgroundPanel = background)

  def drawShapes(boundingBoxOn:Option[Geometry] = None,
                 shapes:List[(Geometry,Option[Color],Option[Color],String)],
                 centers:List[(Long,Long)]): Unit ={
    drawing.drawShapes(boundingBoxOn,shapes,centers)
  }
}