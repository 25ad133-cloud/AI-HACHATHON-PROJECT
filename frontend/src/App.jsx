import React, { useState, useRef, useEffect } from 'react';
import { sendChatMessage } from './services/chatService';
import { 
  Send, 
  Trash2, 
  BookOpen, 
  Clock, 
  FileText, 
  Award, 
  Home, 
  MessageSquare, 
  Bot, 
  User, 
  AlertTriangle, 
  CheckCircle,
  ShieldCheck,
  ChevronRight
} from 'lucide-react';

function App() {
  const [messages, setMessages] = useState([
    {
      id: 'welcome',
      sender: 'bot',
      text: 'Hello! I am your College AI Assistant. You can ask me questions about attendance policy, library timings, exam rules, academic regulations, or hostel guidelines in English, Tamil (தமிழ்), or Tanglish.',
      isWelcome: true
    }
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const messagesEndRef = useRef(null);

  const suggestions = [
    { text: 'What is the minimum attendance?', icon: Clock },
    { text: 'What are the library timings?', icon: BookOpen },
    { text: 'What are the exam rules?', icon: FileText },
    { text: 'What are the academic regulations?', icon: Award },
    { text: 'What are the hostel rules?', icon: Home }
  ];

  // Auto-scroll to the bottom of the chat
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, loading]);

  const handleSend = async (textToSend) => {
    const query = textToSend || input;
    if (!query || !query.trim()) return;

    // Client-side block for empty strings
    setError(null);
    setLoading(true);

    // Add user message to history
    const userMsgId = Date.now().toString();
    const newUserMsg = {
      id: userMsgId,
      sender: 'user',
      text: query.trim()
    };

    setMessages(prev => [...prev, newUserMsg]);
    if (!textToSend) setInput(''); // Clear input if user typed

    try {
      const response = await sendChatMessage(query);
      
      const botMsgId = (Date.now() + 1).toString();
      const newBotMsg = {
        id: botMsgId,
        sender: 'bot',
        text: response.answer,
        found: response.found,
        evidence: response.evidence,
        source: response.source,
        confidence: response.confidence
      };

      setMessages(prev => [...prev, newBotMsg]);
    } catch (err) {
      console.error(err);
      setError(err.message || 'An unexpected error occurred while contacting the server.');
    } finally {
      setLoading(false);
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleClear = () => {
    setMessages([
      {
        id: 'welcome',
        sender: 'bot',
        text: 'Hello! I am your College AI Assistant. You can ask me questions about attendance policy, library timings, exam rules, academic regulations, or hostel guidelines in English, Tamil (தமிழ்), or Tanglish.',
        isWelcome: true
      }
    ]);
    setError(null);
  };

  const renderConfidenceBadge = (score, found) => {
    if (!found || score === undefined) return null;
    
    let colorClass = 'bg-red-500/20 text-red-400 border-red-500/30';
    let label = 'Low Confidence';
    
    if (score >= 0.7) {
      colorClass = 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30';
      label = 'High Confidence';
    } else if (score >= 0.3) {
      colorClass = 'bg-amber-500/20 text-amber-400 border-amber-500/30';
      label = 'Medium Confidence';
    }

    const percentage = Math.round(score * 100);

    return (
      <div className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold border ${colorClass} mt-2`}>
        <ShieldCheck className="w-3 h-3" />
        <span>{label} ({percentage}%)</span>
      </div>
    );
  };

  return (
    <div className="flex flex-col min-h-screen bg-gradient-to-br from-[#0c0f1d] via-[#121528] to-[#0a0c16] text-[#e2e8f0] font-sans antialiased">
      
      {/* Premium Header */}
      <header className="sticky top-0 z-50 flex items-center justify-between px-6 py-4 border-b border-slate-800/60 bg-[#0c0f1d]/85 backdrop-blur-md">
        <div className="flex items-center gap-3">
          <div className="flex items-center justify-center w-10 h-10 rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-600 shadow-lg shadow-blue-500/20">
            <Bot className="w-5 h-5 text-white" />
          </div>
          <div>
            <h1 className="text-lg font-bold tracking-tight text-white flex items-center gap-2">
              CertiTrace <span className="text-xs font-normal px-2 py-0.5 rounded-md bg-blue-500/10 border border-blue-500/25 text-blue-400">College AI Bot</span>
            </h1>
            <p className="text-[10px] text-slate-400">Interactive Knowledge System</p>
          </div>
        </div>
        <button 
          onClick={handleClear}
          className="flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-medium border border-slate-800 bg-slate-900/60 hover:bg-red-950/30 hover:border-red-900/40 hover:text-red-400 transition-all duration-200"
          title="Clear Conversation"
        >
          <Trash2 className="w-3.5 h-3.5" />
          <span>Clear Chat</span>
        </button>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 flex flex-col md:flex-row w-full max-w-7xl mx-auto p-4 md:p-6 gap-6 overflow-hidden">
        
        {/* Chat Interface Panel */}
        <section className="flex-1 flex flex-col rounded-2xl border border-slate-800/80 bg-[#111426]/60 backdrop-blur-sm shadow-xl overflow-hidden relative min-h-[450px] md:h-[calc(100vh-140px)]">
          
          {/* Top Info Banner for Connection Errors */}
          {error && (
            <div className="flex items-center gap-3 px-4 py-3 bg-red-950/40 border-b border-red-900/40 text-red-300 animate-slide-in">
              <AlertTriangle className="w-5 h-5 text-red-400 shrink-0" />
              <div className="text-xs font-medium flex-1">{error}</div>
              <button 
                onClick={() => setError(null)}
                className="text-red-400 hover:text-red-200 text-xs font-bold px-1.5 py-0.5"
              >
                Dismiss
              </button>
            </div>
          )}

          {/* Chat Messages Log */}
          <div className="flex-1 overflow-y-auto p-4 space-y-4 scrollbar-thin scrollbar-thumb-slate-800">
            {messages.map((msg) => (
              <div 
                key={msg.id}
                className={`flex gap-3 max-w-[85%] ${msg.sender === 'user' ? 'ml-auto flex-row-reverse' : 'mr-auto'}`}
              >
                {/* Avatar Icon */}
                <div className={`w-8 h-8 rounded-lg shrink-0 flex items-center justify-center text-xs font-bold ${
                  msg.sender === 'user' 
                    ? 'bg-blue-600/20 border border-blue-500/30 text-blue-400' 
                    : 'bg-indigo-600/20 border border-indigo-500/30 text-indigo-400'
                }`}>
                  {msg.sender === 'user' ? <User className="w-4 h-4" /> : <Bot className="w-4 h-4" />}
                </div>

                {/* Message Bubble */}
                <div className="flex flex-col">
                  <div className={`px-4 py-3 rounded-2xl text-sm leading-relaxed border ${
                    msg.sender === 'user' 
                      ? 'bg-blue-600/90 text-white border-blue-500/40 rounded-tr-none' 
                      : 'bg-slate-900/80 text-slate-200 border-slate-800/80 rounded-tl-none'
                  }`}>
                    {msg.text}
                    {renderConfidenceBadge(msg.confidence, msg.found)}
                  </div>

                  {/* Evidence & Source Panel (under bot responses, only if found) */}
                  {msg.sender === 'bot' && msg.found && (
                    <div className="mt-2.5 p-3 rounded-xl border border-indigo-950/40 bg-indigo-950/20 space-y-2 text-xs animate-fade-in max-w-xl">
                      <div className="flex items-center gap-1.5 font-bold text-indigo-400">
                        <CheckCircle className="w-3.5 h-3.5 text-indigo-400" />
                        <span>Verified Evidence:</span>
                      </div>
                      <blockquote className="border-l-2 border-indigo-500/50 pl-2.5 text-slate-350 italic leading-relaxed">
                        "{msg.evidence}"
                      </blockquote>
                      <div className="flex items-center gap-1 text-[11px] text-slate-400 pt-1">
                        <span className="font-semibold text-slate-300">Document Source:</span>
                        <span className="px-1.5 py-0.5 rounded bg-slate-900 border border-slate-800 font-mono text-indigo-300">{msg.source}</span>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            ))}

            {/* Pulsing Loading Bubble */}
            {loading && (
              <div className="flex gap-3 max-w-[80%] mr-auto">
                <div className="w-8 h-8 rounded-lg shrink-0 flex items-center justify-center bg-indigo-600/20 border border-indigo-500/30 text-indigo-400">
                  <Bot className="w-4 h-4" />
                </div>
                <div className="flex flex-col">
                  <div className="px-4 py-3 bg-slate-900/85 text-slate-300 border border-slate-800/60 rounded-2xl rounded-tl-none flex items-center gap-1">
                    <span className="w-2 h-2 rounded-full bg-blue-500 animate-bounce" style={{ animationDelay: '0ms' }}></span>
                    <span className="w-2 h-2 rounded-full bg-blue-500 animate-bounce" style={{ animationDelay: '150ms' }}></span>
                    <span className="w-2 h-2 rounded-full bg-blue-500 animate-bounce" style={{ animationDelay: '300ms' }}></span>
                  </div>
                </div>
              </div>
            )}
            
            <div ref={messagesEndRef} />
          </div>

          {/* Form and Input Area */}
          <div className="p-4 border-t border-slate-800/60 bg-[#0d1020]/95">
            <div className="flex gap-2 bg-slate-900/90 border border-slate-850 rounded-xl p-1.5 focus-within:border-blue-500/60 transition-all duration-200">
              <input
                type="text"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={handleKeyPress}
                placeholder="Ask about attendance, library hours, exam rules, or hostel curfew..."
                disabled={loading}
                className="flex-1 bg-transparent border-0 outline-none focus:ring-0 px-3 text-sm text-slate-200 placeholder-slate-500"
              />
              <button
                onClick={() => handleSend()}
                disabled={loading || !input.trim()}
                className="p-2 rounded-lg bg-blue-600 text-white hover:bg-blue-500 disabled:opacity-40 disabled:hover:bg-blue-600 transition-all duration-150 shadow shadow-blue-600/20"
              >
                <Send className="w-4 h-4" />
              </button>
            </div>
          </div>
        </section>

        {/* Suggested Queries Sidebar Panel */}
        <aside className="w-full md:w-80 flex flex-col gap-4 rounded-2xl border border-slate-800/80 bg-[#111426]/60 backdrop-blur-sm shadow-xl p-5 md:h-[calc(100vh-140px)]">
          <div>
            <h2 className="text-sm font-bold tracking-wide text-white uppercase mb-1">Suggested Questions</h2>
            <p className="text-xs text-slate-400">Click any policy question below to query the AI Chatbot directly:</p>
          </div>
          <div className="flex-1 flex flex-col gap-2.5 overflow-y-auto mt-2 pr-1 scrollbar-thin">
            {suggestions.map((sug, idx) => {
              const IconComp = sug.icon;
              return (
                <button
                  key={idx}
                  onClick={() => !loading && handleSend(sug.text)}
                  disabled={loading}
                  className="w-full text-left flex items-center gap-3 p-3.5 rounded-xl border border-slate-800 bg-slate-900/50 hover:bg-indigo-950/20 hover:border-indigo-500/40 text-slate-350 hover:text-white transition-all duration-200 group disabled:opacity-50 disabled:hover:bg-slate-900/50 disabled:hover:border-slate-800"
                >
                  <div className="p-2 rounded-lg bg-slate-900 border border-slate-800 group-hover:bg-indigo-950 group-hover:border-indigo-500/30 text-slate-400 group-hover:text-indigo-400 transition-colors">
                    <IconComp className="w-4 h-4" />
                  </div>
                  <div className="flex-1 text-xs font-semibold leading-snug">{sug.text}</div>
                  <ChevronRight className="w-4 h-4 text-slate-600 group-hover:text-indigo-400 group-hover:translate-x-0.5 transition-all" />
                </button>
              );
            })}
          </div>
          
          {/* Database & Security Info block */}
          <div className="p-3.5 rounded-xl bg-slate-900/70 border border-slate-800/80 text-[11px] text-slate-400 leading-normal space-y-2">
            <h3 className="font-semibold text-slate-300">Auditing and Compliance</h3>
            <p>Every response is validated and logged inside the SQLite database audit logs.</p>
            <p className="flex items-center gap-1.5 text-emerald-400/90 font-medium">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-ping"></span>
              Secure Connection Active
            </p>
          </div>
        </aside>

      </main>
    </div>
  );
}

export default App;
