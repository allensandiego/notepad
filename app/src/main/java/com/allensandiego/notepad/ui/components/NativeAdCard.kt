package com.allensandiego.notepad.ui.components

import android.content.Context
import android.graphics.Color as AndroidColor
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button as AndroidButton
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.allensandiego.notepad.BuildConfig
import com.allensandiego.notepad.ui.theme.PrimaryEmerald
import com.allensandiego.notepad.ui.theme.SurfaceDark
import com.allensandiego.notepad.ui.theme.TextLight
import com.allensandiego.notepad.ui.theme.TextMuted
import com.allensandiego.notepad.ui.theme.AccentMint
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.bugsnag.android.Bugsnag

@Composable
fun NativeAdCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }

    val adUnitId = BuildConfig.ADMOB_NATIVE_AD_UNIT_ID

    LaunchedEffect(adUnitId) {
        if (adUnitId == "YOUR_ADMOB_NATIVE_ID" || adUnitId.isEmpty()) {
            isLoading = false
            loadFailed = true
            return@LaunchedEffect
        }

        try {
            val adLoader = AdLoader.Builder(context, adUnitId)
                .forNativeAd { ad ->
                    nativeAd = ad
                    isLoading = false
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        isLoading = false
                        loadFailed = true
                        Bugsnag.leaveBreadcrumb("AdMob: Native Ad failed to load: ${error.message}")
                    }
                })
                .withNativeAdOptions(NativeAdOptions.Builder().build())
                .build()

            adLoader.loadAd(AdRequest.Builder().build())
        } catch (e: Exception) {
            e.printStackTrace()
            isLoading = false
            loadFailed = true
            Bugsnag.notify(e)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            nativeAd?.destroy()
        }
    }

    if (isLoading) {
        NativeAdShimmerPlaceholder(modifier)
    } else if (nativeAd != null && !loadFailed) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.8f))
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                factory = { ctx ->
                    createNativeAdLayout(ctx)
                },
                update = { nativeAdView ->
                    populateNativeAd(nativeAdView, nativeAd!!)
                }
            )
        }
    }
}

@Composable
fun NativeAdShimmerPlaceholder(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = alpha))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(14.dp)
                            .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(10.dp)
                            .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            )
        }
    }
}

private fun createNativeAdLayout(context: Context): NativeAdView {
    val adView = NativeAdView(context).apply {
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    val rootLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // Header layout (Icon, Headline, Advertiser + Ad Attribution Label)
    val headerLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        gravity = Gravity.CENTER_VERTICAL
    }

    // App/Ad Icon ImageView
    val iconView = ImageView(context).apply {
        id = View.generateViewId()
        layoutParams = LinearLayout.LayoutParams(dpToPx(context, 40), dpToPx(context, 40)).apply {
            rightMargin = dpToPx(context, 12)
        }
        scaleType = ImageView.ScaleType.FIT_CENTER
    }
    headerLayout.addView(iconView)

    // Texts Layout (Headline + Advertiser)
    val textLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1f
        )
    }

    val headlineView = TextView(context).apply {
        id = View.generateViewId()
        setTextColor(AndroidColor.WHITE)
        textSize = 15f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        maxLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.END
    }
    textLayout.addView(headlineView)

    val advertiserView = TextView(context).apply {
        id = View.generateViewId()
        setTextColor(AndroidColor.parseColor("#94A3B8")) // TextMuted
        textSize = 12f
        maxLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.END
        setPadding(0, dpToPx(context, 2), 0, 0)
    }
    textLayout.addView(advertiserView)
    headerLayout.addView(textLayout)

    // Ad Attribution Badge/Label (Green border/box)
    val adLabel = TextView(context).apply {
        text = "Ad"
        setTextColor(AndroidColor.parseColor("#34D399")) // AccentMint
        textSize = 10f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(dpToPx(context, 6), dpToPx(context, 2), dpToPx(context, 6), dpToPx(context, 2))
        setBackgroundResource(android.R.drawable.editbox_background_normal) // fallback
        background?.setTint(AndroidColor.parseColor("#0F172A")) // dark background
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = dpToPx(context, 8)
        }
    }
    headerLayout.addView(adLabel)
    rootLayout.addView(headerLayout)

    // Body Text
    val bodyView = TextView(context).apply {
        id = View.generateViewId()
        setTextColor(AndroidColor.parseColor("#E2E8F0")) // Slate 200
        textSize = 13f
        setPadding(0, dpToPx(context, 10), 0, dpToPx(context, 10))
        maxLines = 2
        ellipsize = android.text.TextUtils.TruncateAt.END
    }
    rootLayout.addView(bodyView)

    // Media Content View
    val mediaView = MediaView(context).apply {
        id = View.generateViewId()
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dpToPx(context, 120)
        ).apply {
            bottomMargin = dpToPx(context, 10)
        }
    }
    rootLayout.addView(mediaView)

    // Call To Action (CTA) Button
    val ctaButton = AndroidButton(context).apply {
        id = View.generateViewId()
        setTextColor(AndroidColor.WHITE)
        textSize = 14f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setBackgroundColor(AndroidColor.parseColor("#10B981")) // PrimaryEmerald
        isAllCaps = false
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dpToPx(context, 40)
        )
    }
    rootLayout.addView(ctaButton)

    // Register all views to NativeAdView properties
    adView.headlineView = headlineView
    adView.bodyView = bodyView
    adView.callToActionView = ctaButton
    adView.iconView = iconView
    adView.mediaView = mediaView
    adView.advertiserView = advertiserView

    adView.addView(rootLayout)
    return adView
}

private fun populateNativeAd(adView: NativeAdView, nativeAd: NativeAd) {
    // Populate simple fields
    (adView.headlineView as TextView).text = nativeAd.headline
    (adView.bodyView as TextView).text = nativeAd.body

    // Populate Icon
    if (nativeAd.icon != null) {
        adView.iconView?.visibility = View.VISIBLE
        (adView.iconView as ImageView).setImageDrawable(nativeAd.icon?.drawable)
    } else {
        adView.iconView?.visibility = View.GONE
    }

    // Populate Advertiser
    if (nativeAd.advertiser != null) {
        adView.advertiserView?.visibility = View.VISIBLE
        (adView.advertiserView as TextView).text = nativeAd.advertiser
    } else {
        adView.advertiserView?.visibility = View.GONE
    }

    // Populate Media Content
    if (adView.mediaView != null) {
        val mediaContent = nativeAd.mediaContent
        adView.mediaView?.setMediaContent(mediaContent)
        // If there's no media content (e.g. text-only ad), collapse the media view height
        if (mediaContent == null || (!mediaContent.hasVideoContent() && mediaContent.mainImage == null)) {
            adView.mediaView?.visibility = View.GONE
        } else {
            adView.mediaView?.visibility = View.VISIBLE
        }
    }

    // Populate CTA Button
    if (nativeAd.callToAction != null) {
        adView.callToActionView?.visibility = View.VISIBLE
        (adView.callToActionView as AndroidButton).text = nativeAd.callToAction
    } else {
        adView.callToActionView?.visibility = View.GONE
    }

    // Bind NativeAd object to the ad view container
    adView.setNativeAd(nativeAd)
}

private fun dpToPx(context: Context, dp: Int): Int {
    val density = context.resources.displayMetrics.density
    return (dp * density).toInt()
}
