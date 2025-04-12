
package com.merlin.basketballanimationgame

    import androidx.compose.animation.core.*
    import androidx.compose.foundation.background
    import androidx.compose.foundation.layout.*
    import androidx.compose.material3.Text
    import androidx.compose.runtime.*
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.sp

    @Composable
    fun AnimatedScoreDisplay(score: Int) {
        // Infinite animation for the "Score" label (ease-in, ease-out)
        val infiniteTransition = rememberInfiniteTransition()
        val boxAlpha by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse // Reverse animation for smooth looping
            )
        )

        // Animation for the actual score value (falling from top)
        var triggerAnimation by remember { mutableStateOf(false) }

        // Animation for the actual score value (falling from top)
        val scoreOffsetY by animateFloatAsState(
            targetValue = if (triggerAnimation) 0f else -50f, // Start from above and fall to its position
            animationSpec = tween(durationMillis = 1000, easing = EaseInOut)
        )

        LaunchedEffect(score) {
            // Trigger animations whenever the score changes
            triggerAnimation = false
            triggerAnimation = true
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // "Score" label with blue background and ease-in, ease-out animation
            Box(
                modifier = Modifier
                    .background(Color.Blue.copy(alpha = boxAlpha)) // Animate alpha of the blue box itself

                    .padding(horizontal = 16.dp, vertical = 8.dp), // Blue background around the word "Score"
              //  verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Score",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                //    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.size(8.dp)) // Add space between score and icon


            }

            Spacer(modifier = Modifier.height(16.dp))

            // Actual score value with falling animation
            Box(
                modifier = Modifier.offset(y = scoreOffsetY.dp) // Falling from top animation
            ) {
                Text(
                    text = score.toString(),
                    color = Color.Black,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
