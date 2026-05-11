<template>
  <!-- Top application header with gradient background -->
  <header class="app-header">
    <h1>Campus RAG Demo</h1>
    <!-- Theme selector: allows user to switch between color schemes -->
    <div class="theme-selector">
      <label for="theme-select">Theme:</label>
      <select id="theme-select" v-model="selectedTheme" @change="applyTheme">
        <option value="default">Default</option>
        <option value="purple">Purple</option>
        <option value="orange">Orange</option>
      </select>
    </div>
    <!-- Mode selector: choose between RAG, Agent, LLM‑Wiki, GBrain, HierarchyMemory and HyperMemory modes -->
    <div class="mode-selector">
      <label for="mode-select">Mode:</label>
      <select id="mode-select" v-model="selectedMode">
        <option value="rag">RAG</option>
        <option value="agent">Agent</option>
        <option value="llmwiki">LLM Wiki</option>
        <option value="gbrain">GBrain</option>
        <option value="hierarchy">Hierarchy Memory</option>
        <option value="hyper">Hyper Memory</option>
      </select>
    </div>
  </header>
  <!-- Main content container -->
  <main class="container">
    <section class="upload">
      <h2>Upload Knowledge File</h2>
      <input type="file" @change="onFileChange" />
      <button class="primary-btn" @click="uploadFile" :disabled="!selectedFile">Upload</button>
      <p v-if="uploadStatus">{{ uploadStatus }}</p>
    </section>
    <section class="chat">
      <h2>Ask a Question</h2>
      <!-- Animated message list using transition-group for smooth message appearance -->
      <transition-group name="message" tag="div" class="messages" ref="messageContainer">
        <div
          v-for="msg in messages"
          :key="msg.id"
          :class="['message', msg.role.toLowerCase()]"
        >
          <!-- display the role on its own line for clarity -->
          <strong class="message-label">{{ msg.role }}</strong>
          <span class="message-content">{{ msg.content }}</span>
        </div>
        <!-- Loading spinner shown when waiting for assistant reply -->
        <div v-if="loading" class="spinner" key="spinner"></div>
      </transition-group>
      <div class="input-row">
        <input
          v-model="userInput"
          @keyup.enter="sendQuestion"
          placeholder="Type your question..."
        />
        <button class="primary-btn" @click="sendQuestion" :disabled="!userInput">Send</button>
      </div>
    </section>
  </main>
</template>

<script setup>
import { ref, nextTick, onMounted, watch } from 'vue';

const selectedFile = ref(null);
const uploadStatus = ref('');
const userInput = ref('');
const messages = ref([]);
let messageCounter = 0;
const messageContainer = ref(null);

// Flag for showing a loading spinner while waiting for assistant response
const loading = ref(false);

// Theme selection for dynamic color schemes
const selectedTheme = ref('default');

// Mode selection for toggling between RAG and Agent backends
const selectedMode = ref('rag');

const themes = {
  default: {
    '--primary-color': '#0d47a1',
    '--primary-color-light': '#e3f2fd',
    '--primary-color-dark': '#0a2e74',
    '--secondary-color': '#33691e',
    '--secondary-color-light': '#f1f8e9',
    '--secondary-color-dark': '#234013',
    '--accent-color': '#ff9800',
    '--accent-color-light': '#ffe0b2',
  },
  purple: {
    '--primary-color': '#6a1b9a',
    '--primary-color-light': '#f3e5f5',
    '--primary-color-dark': '#38006b',
    '--secondary-color': '#ad1457',
    '--secondary-color-light': '#fce4ec',
    '--secondary-color-dark': '#78002e',
    '--accent-color': '#7c4dff',
    '--accent-color-light': '#d1c4e9',
  },
  orange: {
    '--primary-color': '#e65100',
    '--primary-color-light': '#ffece5',
    '--primary-color-dark': '#bf360c',
    '--secondary-color': '#004d40',
    '--secondary-color-light': '#e0f2f1',
    '--secondary-color-dark': '#00251a',
    '--accent-color': '#ff6f00',
    '--accent-color-light': '#fff3e0',
  },
};

function applyTheme() {
  const theme = themes[selectedTheme.value];
  Object.keys(theme).forEach((key) => {
    document.documentElement.style.setProperty(key, theme[key]);
  });
}

// Apply the theme on mount and whenever the selection changes
onMounted(() => applyTheme());
watch(selectedTheme, () => applyTheme());

function scrollToBottom() {
  nextTick(() => {
    if (messageContainer.value) {
      messageContainer.value.scrollTop = messageContainer.value.scrollHeight;
    }
  });
}

function onFileChange(event) {
  selectedFile.value = event.target.files[0];
}

async function uploadFile() {
  if (!selectedFile.value) return;
  const formData = new FormData();
  formData.append('file', selectedFile.value);
  uploadStatus.value = 'Uploading...';
  try {
    // Choose the upload endpoint based on the selected mode. When using
    // LLM‑Wiki or GBrain modes we upload to the wiki ingestion endpoint,
    // otherwise we upload to the standard RAG endpoint.
    // Choose upload endpoint based on selected mode.  LLM‑Wiki and GBrain
    // share the wiki upload endpoint, Hierarchy uses its own, Hyper uses
    // its own, and all others use the standard documents endpoint.
    let uploadEndpoint;
    if (selectedMode.value === 'llmwiki' || selectedMode.value === 'gbrain') {
      uploadEndpoint = '/api/wiki/upload';
    } else if (selectedMode.value === 'hierarchy') {
      uploadEndpoint = '/api/hierarchy/upload';
    } else if (selectedMode.value === 'hyper') {
      uploadEndpoint = '/api/hyper/upload';
    } else {
      uploadEndpoint = '/api/documents';
    }
    const resp = await fetch(uploadEndpoint, {
      method: 'POST',
      body: formData,
    });
    if (resp.ok) {
      uploadStatus.value = 'Upload complete and indexed.';
      selectedFile.value = null;
    } else {
      uploadStatus.value = 'Upload failed: ' + resp.statusText;
    }
  } catch (err) {
    uploadStatus.value = 'Upload error: ' + err.message;
  }
}

async function sendQuestion() {
  const input = userInput.value.trim();
  if (!input) return;
  messages.value.push({ id: ++messageCounter, role: 'User', content: input });
  userInput.value = '';
  scrollToBottom();
  const body = JSON.stringify({ conversationId: 'default', userInput: input });
  try {
    // show spinner until we finish reading response
    loading.value = true;
    // Determine the endpoint based on the selected mode. RAG uses /api/chat,
    // Agent uses /api/agent/chat, LLM‑Wiki uses /api/wiki/chat, and GBrain
    // uses /api/gbrain/chat.
    let endpoint;
    if (selectedMode.value === 'agent') {
      endpoint = '/api/agent/chat';
    } else if (selectedMode.value === 'llmwiki') {
      endpoint = '/api/wiki/chat';
    } else if (selectedMode.value === 'gbrain') {
      endpoint = '/api/gbrain/chat';
    } else if (selectedMode.value === 'hierarchy') {
      endpoint = '/api/hierarchy/chat';
    } else if (selectedMode.value === 'hyper') {
      endpoint = '/api/hyper/chat';
    } else {
      endpoint = '/api/chat';
    }
    const resp = await fetch(endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body,
    });
    if (resp.ok) {
      // Stream assistant reply and update message content progressively
      const reader = resp.body.getReader();
      const decoder = new TextDecoder();
      // create an empty assistant message that will be updated on the fly
      const assistantMsg = { id: ++messageCounter, role: 'Assistant', content: '' };
      messages.value.push(assistantMsg);
      scrollToBottom();
      while (true) {
        const { value, done } = await reader.read();
        if (value) {
          assistantMsg.content += decoder.decode(value, { stream: true });
          scrollToBottom();
        }
        if (done) break;
      }
      loading.value = false;
    } else {
      messages.value.push({ id: ++messageCounter, role: 'Assistant', content: 'Error: ' + resp.statusText });
      loading.value = false;
    }
  } catch (err) {
    messages.value.push({ id: ++messageCounter, role: 'Assistant', content: 'Error: ' + err.message });
    loading.value = false;
  }
}
</script>

<style scoped>
.spinner {
  width: 24px;
  height: 24px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #3498db;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 8px auto;
}

/* Theme variables: define primary, secondary, and accent colors for easy customization */
:root {
  --primary-color: #0d47a1;
  --primary-color-light: #e3f2fd;
  --primary-color-dark: #0a2e74;
  --secondary-color: #33691e;
  --secondary-color-light: #f1f8e9;
  --secondary-color-dark: #234013;
  --accent-color: #ff9800;
  --accent-color-light: #ffe0b2;
}

/* Application header with gradient */
.app-header {
  background: linear-gradient(135deg, #6a11cb 0%, #2575fc 100%);
  color: #ffffff;
  padding: 24px 0;
  text-align: center;
  margin-bottom: 32px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.app-header h1 {
  margin: 0;
  font-size: 2rem;
  font-weight: bold;
}

/* Theme selector inside header */
.theme-selector {
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.theme-selector label {
  color: #ffffff;
  font-size: 0.9rem;
}
.theme-selector select {
  padding: 4px 8px;
  border-radius: 4px;
  border: none;
  font-size: 0.9rem;
  cursor: pointer;
}

/* Mode selector styling */
.mode-selector {
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.mode-selector label {
  color: #ffffff;
  font-size: 0.9rem;
}
.mode-selector select {
  padding: 4px 8px;
  border-radius: 4px;
  border: none;
  font-size: 0.9rem;
  cursor: pointer;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
/* Main container for content */
.container {
  max-width: 800px;
  margin: 0 auto;
  padding: 16px;
  font-family: Arial, sans-serif;
  /* Use primary text color for general text */
  color: var(--primary-color);
}


/* Section headings */
section h2 {
  margin-top: 0;
  margin-bottom: 16px;
  color: var(--primary-color-dark);
}

.upload,
.chat {
  border: 1px solid #ccc;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 24px;
  background-color: #ffffff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

/* Chat window styling */
.messages {
  height: 300px;
  border: 1px solid #eee;
  padding: 8px;
  margin-bottom: 8px;
  overflow-y: auto;
  background-color: #fafafa;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

/* Generic message bubble */
.message {
  max-width: 70%;
  padding: 8px 12px;
  border-radius: 12px;
  line-height: 1.4;
  word-wrap: break-word;
}

/* Label and content separation */
.message-label {
  display: block;
  font-weight: bold;
  margin-bottom: 2px;
}
.message-content {
  white-space: pre-wrap;
}

/* User message specific styles */
.message.user {
  align-self: flex-end;
  background-color: var(--primary-color-light);
  color: var(--primary-color);
}

/* Assistant message specific styles */
.message.assistant {
  align-self: flex-start;
  background-color: var(--secondary-color-light);
  color: var(--secondary-color);
}

.input-row {
  display: flex;
  gap: 8px;
}
.input-row input {
  flex: 1;
  padding: 8px;
  border: 1px solid #ccc;
  border-radius: 4px;
}

/* Primary button styling used throughout the app */
.primary-btn {
  background-color: var(--primary-color);
  color: #ffffff;
  border: none;
  border-radius: 4px;
  padding: 8px 16px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.primary-btn:hover:not(:disabled) {
  background-color: var(--primary-color-dark);
}

.primary-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Message list transition animations */
.message-enter-active,
.message-leave-active {
  transition: all 0.25s ease;
}
.message-enter-from,
.message-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
.message-enter-to,
.message-leave-from {
  opacity: 1;
  transform: translateY(0);
}
</style>