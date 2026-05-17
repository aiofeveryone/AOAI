package com.aoai.chat.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aoai.chat.R
import com.aoai.chat.data.ChatMessage
import com.aoai.chat.data.Role

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHistoryBottomSheet(
    messages: List<ChatMessage>,
    sheetState: SheetState,
    onMessageClick: (ChatMessage) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
            Text(
                text = stringResource(R.string.recent_conversations),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                items(messages.filter { it.role == Role.USER && !it.isHidden }.reversed()) { msg ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = msg.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier.clickable {
                            onMessageClick(msg)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}
