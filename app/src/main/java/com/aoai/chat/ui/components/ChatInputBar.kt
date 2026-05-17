package com.aoai.chat.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aoai.chat.R
import com.aoai.chat.data.ChatMessage

@Composable
fun ChatInputBar(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    isSending: Boolean,
    isOnline: Boolean,
    replyingTo: ChatMessage?,
    onCancelReply: () -> Unit,
    onSend: (String) -> Unit,
    onCancelCurrent: () -> Unit,
    onMicClick: () -> Unit,
    onTtsClick: () -> Unit,
    onLanguageClick: () -> Unit,
    isSpeaking: Boolean,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Upper row with utility buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMicClick) { 
                Icon(Icons.Filled.Mic, null) 
            }
            IconButton(onClick = onTtsClick) {
                Icon(
                    imageVector = if (isSpeaking) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                    contentDescription = stringResource(R.string.tts_read_aloud),
                    tint = if (isSpeaking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onLanguageClick) { 
                Icon(Icons.Filled.Language, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) 
            }
        }

        // Reply preview
        if (replyingTo != null) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.Reply, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = replyingTo.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onCancelReply, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Main input row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChange,
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.chat_placeholder)) },
                maxLines = 5,
                trailingIcon = {
                    if (inputText.isNotEmpty()) {
                        IconButton(onClick = { onInputTextChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear text", modifier = Modifier.size(20.dp))
                        }
                    }
                },
                enabled = isOnline
            )
            Spacer(Modifier.width(8.dp))

            if (isSending && inputText.isEmpty()) {
                IconButton(
                    onClick = onCancelCurrent,
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Filled.Stop, null, tint = MaterialTheme.colorScheme.onError)
                }
            } else {
                if (isOnline && (inputText.isNotEmpty() || replyingTo != null)) {
                    IconButton(
                        onClick = { onSend(inputText) },
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.iconButtonColors(containerColor = if (isSending) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Filled.Send, null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}
