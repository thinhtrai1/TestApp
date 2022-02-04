package com.test

import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.test.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)


        val layoutManager = FlexboxLayoutManager(this, FlexDirection.ROW)
        val adapter = FlexAdapter()
        mBinding.rcvFlexBox.adapter = adapter
        mBinding.rcvFlexBox.layoutManager = layoutManager
        mBinding.rcvFlexBox.post { // for last line no grow
            val itemCount = layoutManager.flexLines.lastOrNull()?.itemCount ?: return@post
            adapter.noGrowIndex = adapter.randoms.size - itemCount
            adapter.notifyItemRangeChanged(adapter.noGrowIndex, itemCount)
        }


        mBinding.rcvSwipe.adapter = SwipeAdapter(mBinding.rcvSwipe)
        mBinding.rcvSwipe.layoutManager = LinearLayoutManager(this)


        mBinding.imvAnimated.setOnClickListener {
            (mBinding.imvAnimated.drawable as AnimatedVectorDrawable).start()
            WheelBottomSheetDialog.newInstance().show(supportFragmentManager, null)
        }


        mBinding.btnTest.setOnClickListener {
            SpotlightView(this)
                .add(
                    type = SpotlightView.SpotlightType.Rectangle(),
                    focusView = mBinding.btnTest,
                    tutorialView = mBinding.rcvFlexBox[0],
                    isTutorialInTop = true
                )
                .add(
                    type = SpotlightView.SpotlightType.Circle,
                    focusView = mBinding.imvAnimated,
                    tutorialView = mBinding.rcvFlexBox[1],
                    isTutorialInTop = false
                )
                .add(
                    type = SpotlightView.SpotlightType.Rectangle(8),
                    focusView = mBinding.btnTest,
                    tutorialView = mBinding.rcvFlexBox[2],
                    isTutorialInTop = true
                )
                .start()
        }
    }

    private fun isPermissionGranted(vararg permissions: String): Boolean {
        return permissions.indexOfFirst { ContextCompat.checkSelfPermission(this, it) != 0 } == -1
    }

    companion object {
        val DATA = listOf("Thinh", "Nguyen", "Duc", "Duc Thinh", "Nguyen Duc Thinh")
    }
}