package oscar.anytime.lns.disabled_benchs.productmatrixtsp

import oscar.anytime.lns.models.ProductMatrixTSP

/**
  * Created by pschaus on 8/05/17.
  */
object PMTSP_4 extends App {

  new ProductMatrixTSP("data/pmtsp/pmtsp-4.txt", 4610).main(args)

}
