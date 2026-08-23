const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

/**
 * Sends a chat question to the Java HTTP server backend.
 * @param {string} question 
 * @returns {Promise<Object>} The backend JSON response: { answer, found, evidence, source, confidence }
 */
export const sendChatMessage = async (question) => {
  if (!question || !question.trim()) {
    throw new Error('Question cannot be empty.');
  }

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 15000); // 15 seconds timeout

  try {
    const response = await fetch(`${API_URL}/api/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ question: question.trim() }),
      signal: controller.signal,
    });

    clearTimeout(timeoutId);

    let data;
    const contentType = response.headers.get('Content-Type');
    if (contentType && contentType.includes('application/json')) {
      data = await response.json();
    } else {
      const text = await response.text();
      throw new Error(text || 'Empty or invalid response from server.');
    }

    if (!response.ok) {
      // Check if server returned a structured error in the response
      if (data && data.answer) {
        return data;
      }
      throw new Error(`Server error: status ${response.status}`);
    }

    return data;
  } catch (error) {
    clearTimeout(timeoutId);
    if (error.name === 'AbortError') {
      throw new Error('Request timed out after 15 seconds. Please check if the backend server is lagging.');
    }
    // Network errors (like server down)
    if (error.message.includes('Failed to fetch') || error.message.includes('NetworkError') || error.code === 'ERR_CONNECTION_REFUSED') {
      throw new Error('Could not connect to the backend server. Please verify the Spring Boot/Java backend is running at ' + API_URL);
    }
    throw error;
  }
};
