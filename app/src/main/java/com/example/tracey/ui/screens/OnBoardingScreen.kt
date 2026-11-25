package com.example.tracey.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tracey.R
import com.example.tracey.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Screen 1: Opening Welcome Screen
 */
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onReturningUserLogin: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = PersianMistBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top subtle pill badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PureWhiteSurface,
                    border = BorderStroke(1.dp, SilverBorder.copy(alpha = 0.7f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = CharcoalPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tracey • PHP (₱)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = DeepGraphite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.6f))

            Text(
                text = "Hello! My name is Tracey and I will be\nyour personal budget assistant.",
                style = MaterialTheme.typography.bodyLarge,
                color = MutedFurGrey,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Persian Grey Cat Mascot Illustration
            TraceyCatMascot(modifier = Modifier.size(190.dp))

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Track Smarter\nWith Tracey",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 32.sp,
                    lineHeight = 38.sp
                ),
                fontWeight = FontWeight.ExtraBold,
                color = CharcoalPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CharcoalPrimary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Get Started",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Returning user? Log in here",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = DeepGraphite,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clickable { onReturningUserLogin() }
                    .padding(vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Screen 2: 5-Page Swipeable Onboarding Walkthrough
 */
@Composable
fun OnboardingPagerScreen(
    onFinishOnboarding: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = PersianMistBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Top Skip Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onFinishOnboarding) {
                    Text(
                        text = "Skip",
                        color = MutedFurGrey,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Swipeable Pages
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(pageIndex = page)
            }

            // Bottom Dash Indicators + Circular Next Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                // 5 Horizontal Dash Indicators Centered
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(5) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 34.dp else 18.dp,
                            label = "dashWidth"
                        )
                        val color by animateColorAsState(
                            targetValue = if (isSelected) CharcoalPrimary else SilverBorder,
                            label = "dashColor"
                        )
                        Box(
                            modifier = Modifier
                                .height(3.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }

                // Circular Next Button on Bottom Right
                Surface(
                    shape = CircleShape,
                    color = CharcoalPrimary,
                    modifier = Modifier
                        .size(52.dp)
                        .align(Alignment.CenterEnd)
                        .clickable {
                            if (pagerState.currentPage < 4) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            } else {
                                onFinishOnboarding()
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(pageIndex: Int) {
    val titles = listOf(
        "Money Disappears Before\nYou Know It?",
        "Take Control Of Your\nMoney Today",
        "Add Expenses\nEffortlessly",
        "Track Paid & Pending\nBills Clearly",
        "Get Smart Budget\nOverview"
    )
    val subtitles = listOf(
        "You forget to track daily spending, bills pile up, and you're never sure where your allowance really goes.",
        "Log expenses in seconds, see where your money goes, and stay on top of your budget — stress-free.",
        "Record your spending by category and date — fast, clean, and organized in one tap.",
        "Tag transactions as Paid or Pending and filter your history anytime so no due date slips past.",
        "Monitor your monthly limit and remaining balance at a glance with real-time progress tracking."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = titles[pageIndex],
            style = MaterialTheme.typography.headlineMedium.copy(lineHeight = 34.sp),
            fontWeight = FontWeight.Bold,
            color = DeepGraphite,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = subtitles[pageIndex],
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = MutedFurGrey,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Minimalist Preview Card for Each Slide
        when (pageIndex) {
            0 -> {
                PreviewIllustrationCard(
                    icon = Icons.Default.HelpOutline,
                    badgeText = "Where did ₱1,500 go?",
                    badgeBg = AmberPendingBg,
                    badgeColor = AmberPending,
                    caption = "Unlogged snacks, rides & subscriptions add up fast."
                )
            }
            1 -> {
                PreviewIllustrationCard(
                    icon = Icons.Default.VerifiedUser,
                    badgeText = "Budget On Track",
                    badgeBg = SagePaidBg,
                    badgeColor = SagePaid,
                    caption = "Every peso accounted for in a calm, clutter-free space."
                )
            }
            2 -> {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = PureWhiteSurface,
                    border = BorderStroke(1.dp, SilverBorder.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Recorded: Expense", style = MaterialTheme.typography.labelSmall, color = MutedFurGrey)
                            Text("Today", style = MaterialTheme.typography.labelSmall, color = MutedFurGrey)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = PersianCoatGrey,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Restaurant, contentDescription = null, tint = CharcoalPrimary)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Food & Drinks", fontWeight = FontWeight.Bold, color = DeepGraphite)
                                Text("Dinner with friends", style = MaterialTheme.typography.bodySmall, color = MutedFurGrey)
                            }
                            Text("₱250.00", fontWeight = FontWeight.Bold, color = DeepGraphite)
                        }
                    }
                }
            }
            3 -> {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = PureWhiteSurface,
                    border = BorderStroke(1.dp, SilverBorder.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SampleStatusRow("Dorm Internet Bill", "Bills • ₱999.00", "Paid", SagePaid, SagePaidBg)
                        HorizontalDivider(color = PersianCoatGrey)
                        SampleStatusRow("Capstone Printing", "School • ₱180.00", "Pending", AmberPending, AmberPendingBg)
                    }
                }
            }
            4 -> {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = PureWhiteSurface,
                    border = BorderStroke(1.dp, SilverBorder.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("MONTHLY SUMMARY", style = MaterialTheme.typography.labelSmall, color = MutedFurGrey, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("₱2,450.00", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = DeepGraphite)
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { 0.49f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = CharcoalPrimary,
                            trackColor = PersianCoatGrey
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Remaining: ₱2,550.00 of ₱5,000.00 budget", style = MaterialTheme.typography.bodySmall, color = SagePaid, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1.2f))
    }
}

@Composable
private fun PreviewIllustrationCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badgeText: String,
    badgeBg: Color,
    badgeColor: Color,
    caption: String
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = PureWhiteSurface,
        border = BorderStroke(1.dp, SilverBorder.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = PersianCoatGrey,
                modifier = Modifier.size(68.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(34.dp), tint = CharcoalPrimary)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Surface(shape = RoundedCornerShape(10.dp), color = badgeBg) {
                Text(
                    text = badgeText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = badgeColor
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MutedFurGrey,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SampleStatusRow(
    title: String,
    subtitle: String,
    status: String,
    statusColor: Color,
    statusBg: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, color = DeepGraphite)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MutedFurGrey)
        }
        Surface(shape = RoundedCornerShape(8.dp), color = statusBg) {
            Text(
                text = status,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
        }
    }
}

/**
 * Shared Grey Cat Mascot matching the App Icon (used across Welcome, Login, Home, Ask Tracey & About)
 */
@Composable
fun TraceyCatMascot(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(percent = 24),
        border = BorderStroke(1.5.dp, SilverBorder),
        shadowElevation = 2.dp
    ) {
        Image(
            painter = painterResource(id = R.drawable.tracey_cat),
            contentDescription = "Tracey Cat Mascot",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}