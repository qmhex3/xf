package com.frameout.app.splash

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.vector.PathParser

internal const val P_BACK =
    "M730 388.9 L782.3 382.7 Q822 378 818.6 417.9 L793.4 712.1 Q790 752 750.1 754.2 L331 776.9 Q310 778 309.4 757 L308.8 736  L344.8 736 L344.8 738 Q344.9 740.1 346.9 739.9 L736.6 718.8 Q756.8 717.7 758.5 697.6 L780.6 439.1 Q782.4 419 762.3 421.3 L730 425.2  Z"
internal const val P_PLATE_MID =
    "M226.7 371.2 Q228.3 321.2 278 315.7 L709.3 268 Q759 262.5 755.4 312.4 L732.8 625.2 Q729.2 675.1 679.4 679.4 L265.4 714.8 Q215.6 719.1 217.2 669.1 Z"
internal const val P_MID =
    "M668 284.6 L707 280.3 Q746 276 743.2 315.1 L720.9 624.1 Q718 664 678.1 667.4 L265.9 702.8 Q228 706 229.2 668 L230.4 630  L268.5 630 L267.9 647.2 Q267.4 664.5 284.5 663 L663.4 630.5 Q682.4 628.9 683.8 609.9 L703.5 337.3 Q704.8 318.8 686.4 320.8 L668 322.9  Z"
internal const val P_PLATE_FRONT =
    "M160.8 303.6 Q162.5 251.6 214.1 245.9 L649.7 197.7 Q701.3 192 697.6 243.8 L674.7 560.9 Q671 612.8 619.2 617.2 L201.2 653 Q149.4 657.5 151.1 605.5 Z"
internal const val P_RING =
    "M176.7 306 Q178 266 217.8 261.6 L644.2 214.4 Q684 210 681.1 249.9 L658.9 558.1 Q656 598 616.1 601.4 L205.9 636.6 Q166 640 167.3 600 Z M220.3 319.5 Q220.8 305.5 234.7 304 L622.4 261.1 Q636.3 259.5 635.3 273.5 L615.8 543.4 Q614.8 557.4 600.9 558.6 L225.5 590.7 Q211.6 591.9 212 577.9 Z"
internal const val P_SCREEN =
    "M220.3 319.5 Q220.8 305.5 234.7 304 L622.4 261.1 Q636.3 259.5 635.3 273.5 L615.8 543.4 Q614.8 557.4 600.9 558.6 L225.5 590.7 Q211.6 591.9 212 577.9 Z"
internal const val P_MOUNTAIN =
    "M272 549.1 Q262 550 267 541.4 L343.9 409.8 Q352 396 361.6 408.8 L392 449 Q398 457 405.9 450.9 L424.9 436.5 Q436 428 444.2 439.3 L502.1 518.9 Q508 527 498 527.9 Z"

internal val SUN_CENTER = Offset(503f, 362f)
internal const val SUN_R = 31f
internal val FRAME_PIVOT = Offset(424f, 430f)
internal const val ICON_W = 671f
internal const val ICON_CX = 486f
internal const val ICON_CY = 488f

internal class SplashPaths {
    val back = parse(P_BACK)
    val plateMid = parse(P_PLATE_MID)
    val mid = parse(P_MID)
    val plateFront = parse(P_PLATE_FRONT)
    val ring = parse(P_RING).apply { fillType = PathFillType.EvenOdd }
    val screen = parse(P_SCREEN)
    val mountain = parse(P_MOUNTAIN)

    private fun parse(data: String): Path = PathParser().parsePathString(data).toPath()
}
