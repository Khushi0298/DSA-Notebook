import { Fragment, useEffect, useState } from "react";
import { api, getEmail, getToken, setToken } from "./api";

const TOPICS = ["Array", "String", "Hash Map", "Two Pointers", "Sliding Window", "Binary Search", "Linked List", "Stack", "Queue", "Heap", "Tree", "Graph", "Recursion", "Backtracking", "Dynamic Programming", "Greedy", "Sorting", "Math", "Other"];

const today = () => {
  const d = new Date();
  return d.getFullYear() + "-" + String(d.getMonth() + 1).padStart(2, "0") + "-" + String(d.getDate()).padStart(2, "0");
};

function toggleTheme() {
  const dark = getComputedStyle(document.documentElement).colorScheme === "dark";
  document.documentElement.dataset.theme = dark ? "light" : "dark";
}

function Header({ children }) {
  return (
    <div className="top">
      <div>
        <h1>DSA Notebook</h1>
        <p className="sub">Write down what you solved, the trick that made it click, and let spaced revision bring it back before you forget.</p>
      </div>
      <div className="acts">
        {children}
        <button className="theme" onClick={toggleTheme} aria-label="Toggle dark mode">Theme</button>
      </div>
    </div>
  );
}

function Auth({ onDone }) {
  const [mode, setMode] = useState("login");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError("");
    setBusy(true);
    const body = Object.fromEntries(new FormData(e.target));
    try {
      const r = await api("/api/auth/" + mode, { method: "POST", body });
      setToken(r.token, r.email);
      onDone();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <form className="card f auth" onSubmit={submit}>
      <h2>{mode === "login" ? "Log in" : "Create your account"}</h2>
      {error && <div className="err" role="alert">{error}</div>}
      <label>Email<input name="email" type="email" required autoComplete="email" /></label>
      <label>Password <small>(at least 8 characters)</small>
        <input name="password" type="password" required minLength={8} autoComplete={mode === "login" ? "current-password" : "new-password"} />
      </label>
      <div className="row">
        <button className="btn" disabled={busy}>{busy ? "Please wait…" : mode === "login" ? "Log in" : "Sign up"}</button>
        <button type="button" className="link" onClick={() => { setMode(mode === "login" ? "register" : "login"); setError(""); }}>
          {mode === "login" ? "New here? Create an account" : "Already have an account? Log in"}
        </button>
      </div>
      {busy && <p className="sub">The free server may take up to a minute to wake up after being idle.</p>}
    </form>
  );
}

const Head = ({ p }) => (
  <>
    <div className="tags"><span className="tag t">{p.topic}</span><span className={"tag " + p.difficulty}>{p.difficulty}</span></div>
    <h2>{p.title} {p.link && <a className="tag" href={p.link} target="_blank" rel="noopener noreferrer">Open problem</a>}</h2>
  </>
);

const Answer = ({ p }) => (
  <>
    {p.brute && <><div className="lbl">Brute force</div><div className="txt">{p.brute}</div></>}
    <div className="lbl">Optimal approach</div><div className="txt">{p.optimal}</div>
    {(p.time || p.space) && <><div className="lbl">Complexity</div><div className="cx">Time {p.time || "?"} · Space {p.space || "?"}</div></>}
    {p.trick && <div className="trick"><b>The trick</b>{p.trick}</div>}
    {p.code && <><div className="lbl">Code</div><pre>{p.code}</pre></>}
  </>
);

const fmt = (d) => (d ? new Date(d).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" }) : "Never");

function Admin() {
  const [users, setUsers] = useState([]);
  const [stats, setStats] = useState(null);
  const [error, setError] = useState("");
  const [open, setOpen] = useState(null);
  const [detail, setDetail] = useState([]);
  const [myEmail] = useState(getEmail());

  const load = () =>
    Promise.all([api("/api/admin/users"), api("/api/admin/stats")])
      .then(([u, s]) => { setUsers(u); setStats(s); })
      .catch((e) => setError(e.message));
  useEffect(() => { load(); }, []);

  const change = async (path, body) => {
    setError("");
    try { await api(path, { method: "PUT", body }); await load(); } catch (e) { setError(e.message); }
  };

  const toggleDetail = async (u) => {
    if (open === u.id) { setOpen(null); return; }
    try { setDetail(await api(`/api/admin/users/${u.id}/problems`)); setOpen(u.id); } catch (e) { setError(e.message); }
  };

  return (
    <>
      {error && <div className="err" role="alert">{error}</div>}
      {stats && (
        <div className="card">
          <div className="tags">
            <span className="tag t">{stats.users} users</span>
            <span className="tag">{stats.admins} admins</span>
            <span className="tag">{stats.disabled} disabled</span>
            <span className="tag">{stats.problems} problems solved</span>
          </div>
          <div className="lbl">Most practised topics</div>
          <div className="tags">
            {Object.entries(stats.topics).slice(0, 8).map(([t, n]) => <span className="tag" key={t}>{t} · {n}</span>)}
            {Object.keys(stats.topics).length === 0 && <span className="sub">No problems yet.</span>}
          </div>
        </div>
      )}
      <div className="tbl-wrap">
        <table className="tbl">
          <thead>
            <tr><th>User</th><th>Role</th><th>Status</th><th>Joined</th><th>Last login</th><th>Problems</th><th>Actions</th></tr>
          </thead>
          <tbody>
            {users.map((u) => {
              const self = u.email === myEmail;
              return (
                <Fragment key={u.id}>
                  <tr>
                    <td><strong>{u.email}</strong>{self && <span className="who"> (you)</span>}</td>
                    <td><span className={"pill " + u.role}>{u.role}</span></td>
                    <td><span className={"pill " + (u.enabled ? "ON" : "OFF")}>{u.enabled ? "Active" : "Disabled"}</span></td>
                    <td>{fmt(u.createdAt)}</td>
                    <td>{fmt(u.lastLogin)}</td>
                    <td>{u.problems} <span className="who">({u.mastered} well remembered)</span></td>
                    <td className="acts">
                      <button className="btn sec sm" onClick={() => toggleDetail(u)}>{open === u.id ? "Hide" : "View"}</button>
                      <button className="btn sec sm" disabled={self}
                        onClick={() => change(`/api/admin/users/${u.id}/role`, { role: u.role === "ADMIN" ? "USER" : "ADMIN" })}>
                        {u.role === "ADMIN" ? "Make user" : "Make admin"}
                      </button>
                      <button className="btn sec sm" disabled={self}
                        onClick={() => change(`/api/admin/users/${u.id}/enabled`, { enabled: !u.enabled })}>
                        {u.enabled ? "Disable" : "Enable"}
                      </button>
                    </td>
                  </tr>
                  {open === u.id && (
                    <tr>
                      <td colSpan="7" className="detail">
                        {detail.length === 0 ? <span className="sub">This user has not added any problems yet.</span> : (
                          <ul>
                            {detail.map((p) => (
                              <li key={p.id}>
                                <strong>{p.title}</strong> <span className="tag">{p.topic}</span> <span className={"tag " + p.difficulty}>{p.difficulty}</span>
                                <span className="who"> added {fmt(p.created)} · next review {fmt(p.due)} · stage {p.stage + 1} of 6</span>
                              </li>
                            ))}
                          </ul>
                        )}
                      </td>
                    </tr>
                  )}
                </Fragment>
              );
            })}
          </tbody>
        </table>
      </div>
      <p className="sub">Admins see which problems each user added and how well they remember them. Notes and code stay private to the user.</p>
    </>
  );
}

function Notebook({ onLogout, me }) {
  const isAdmin = me?.role === "ADMIN";
  const [items, setItems] = useState([]);
  const [ready, setReady] = useState(false);
  const [error, setError] = useState("");
  const [view, setView] = useState("revise");
  const [editId, setEditId] = useState(null);
  const [revealed, setRevealed] = useState(false);
  const [forced, setForced] = useState(null);
  const [q, setQ] = useState("");
  const [tf, setTf] = useState("All");

  const load = () => api("/api/problems").then(setItems).catch((e) => setError(e.message)).finally(() => setReady(true));
  useEffect(() => { load(); }, []);

  const due = items.filter((p) => p.due <= today()).sort((a, b) => a.due.localeCompare(b.due));
  const current = forced ? items.find((p) => p.id === forced) : due[0];
  const mastered = items.filter((p) => p.stage >= 5).length;

  const go = (v) => { setView(v); setEditId(null); setForced(null); setRevealed(false); setError(""); };

  const rate = async (ok) => {
    try {
      const u = await api(`/api/problems/${current.id}/review?remembered=${ok}&today=${today()}`, { method: "POST" });
      setItems(items.map((p) => (p.id === u.id ? u : p)));
      setForced(null);
      setRevealed(false);
    } catch (e) { setError(e.message); }
  };

  const submit = async (e) => {
    e.preventDefault();
    const body = { ...Object.fromEntries(new FormData(e.target)), created: today() };
    try {
      if (editId) await api("/api/problems/" + editId, { method: "PUT", body });
      else await api("/api/problems", { method: "POST", body });
      await load();
      setEditId(null);
      setView("all");
    } catch (err) { setError(err.message); }
  };

  const remove = async (id) => {
    if (!confirm("Delete this problem?")) return;
    try { await api("/api/problems/" + id, { method: "DELETE" }); setItems(items.filter((p) => p.id !== id)); }
    catch (e) { setError(e.message); }
  };

  const exportJson = () => {
    const a = document.createElement("a");
    a.href = URL.createObjectURL(new Blob([JSON.stringify(items, null, 2)], { type: "application/json" }));
    a.download = "dsa-notebook-backup.json";
    a.click();
  };

  const importJson = (file) => {
    const reader = new FileReader();
    reader.onload = async () => {
      try {
        const r = await api("/api/problems/import", { method: "POST", body: JSON.parse(reader.result) });
        alert(`Imported ${r.imported} problems.`);
        load();
      } catch (e) { setError(e.message || "That file is not a valid backup."); }
    };
    reader.readAsText(file);
  };

  const filtered = items.filter((p) =>
    (tf === "All" || p.topic === tf) && (p.title + " " + p.statement + " " + (p.trick || "")).toLowerCase().includes(q.toLowerCase()));

  const editing = items.find((p) => p.id === editId) || { topic: "Array", difficulty: "Medium" };
  const tabs = [["revise", `Revise (${due.length})`], ["all", "All problems"], ["form", editId ? "Edit problem" : "Add problem"],
    ...(isAdmin ? [["admin", "Admin"]] : [])];

  return (
    <>
      <Header>
        <span className="who">{getEmail()}{isAdmin && " · Admin"}</span>
        <button className="theme" onClick={onLogout}>Log out</button>
      </Header>
      <div className="stats">
        <div><b>{items.length}</b><span>Problems saved</span></div>
        <div><b>{due.length}</b><span>Due for revision</span></div>
        <div><b>{mastered}</b><span>Well remembered</span></div>
      </div>
      <nav>{tabs.map(([v, label]) => <button key={v} className={view === v ? "on" : ""} onClick={() => go(v)}>{label}</button>)}</nav>
      {error && <div className="err" role="alert">{error}</div>}

      {!ready ? (
        <div className="empty">Loading your notebook. The free server can take up to a minute to wake up after being idle.</div>
      ) : view === "revise" ? (
        current ? (
          <>
            <div className="card">
              <Head p={current} />
              <div className="lbl">Problem</div><div className="txt">{current.statement}</div>
              {revealed ? (
                <>
                  <Answer p={current} />
                  <div className="row">
                    <button className="btn bad" onClick={() => rate(false)}>I forgot it</button>
                    <button className="btn ok" onClick={() => rate(true)}>I remembered it</button>
                  </div>
                </>
              ) : (
                <>
                  <p className="sub" style={{ marginBottom: 0 }}>Try to recall the approach, the complexity and the trick before you look.</p>
                  <div className="row"><button className="btn" onClick={() => setRevealed(true)}>Show answer</button></div>
                </>
              )}
            </div>
            <p className="due">{forced ? "Extra practice" : due.length + " due today"}</p>
          </>
        ) : (
          <div className="empty">
            <b>{items.length ? "Nothing to revise today." : "No problems yet."}</b>
            <p>{items.length ? "Come back tomorrow, or test yourself on a random one now." : "Add your first solved problem and it will show up here when it’s time to revise it."}</p>
            {items.length
              ? <button className="btn" onClick={() => { setForced(items[Math.floor(Math.random() * items.length)].id); setRevealed(false); }}>Revise a random problem</button>
              : <button className="btn" onClick={() => go("form")}>Add a problem</button>}
          </div>
        )
      ) : view === "all" ? (
        <>
          <div className="tools">
            <input type="search" placeholder="Search by title, statement or trick" value={q} onChange={(e) => setQ(e.target.value)} aria-label="Search" />
            <select value={tf} onChange={(e) => setTf(e.target.value)} aria-label="Filter by topic">
              <option>All</option>{TOPICS.map((t) => <option key={t}>{t}</option>)}
            </select>
          </div>
          {filtered.length ? filtered.map((p) => (
            <details className="card" key={p.id}>
              <summary>
                <strong>{p.title}</strong>
                <span className="tags"><span className="tag t">{p.topic}</span><span className={"due" + (p.due <= today() ? " now" : "")}>{p.due <= today() ? "Due now" : "Next: " + p.due}</span></span>
              </summary>
              <div className="lbl">Problem</div><div className="txt">{p.statement}</div>
              <Answer p={p} />
              <div className="row">
                <button className="btn sec sm" onClick={() => { setEditId(p.id); setView("form"); }}>Edit</button>
                <button className="btn sec sm" onClick={() => remove(p.id)}>Delete</button>
              </div>
            </details>
          )) : <div className="empty">No problems match. Add one or clear the search.</div>}
          <div className="row">
            <button className="btn sec sm" onClick={exportJson}>Export backup</button>
            <label className="btn sec sm" style={{ cursor: "pointer" }}>Import backup
              <input type="file" accept=".json" hidden onChange={(e) => e.target.files[0] && importJson(e.target.files[0])} />
            </label>
          </div>
          <p className="sub">Your notes are saved to your account in the cloud. You can still export a backup file any time. Importing also works with backups from the browser-only version.</p>
        </>
      ) : view === "admin" && isAdmin ? (
        <Admin />
      ) : (
        <form className="card f" key={editId || "new"} onSubmit={submit}>
          <label>Problem title<input name="title" required defaultValue={editing.title} placeholder="e.g. Longest Substring Without Repeating Characters" /></label>
          <div className="two">
            <label>Data structure or topic<select name="topic" defaultValue={editing.topic}>{TOPICS.map((t) => <option key={t}>{t}</option>)}</select></label>
            <label>Difficulty<select name="difficulty" defaultValue={editing.difficulty}>{["Easy", "Medium", "Hard"].map((t) => <option key={t}>{t}</option>)}</select></label>
          </div>
          <label>Link <small>(optional)</small><input name="link" type="url" defaultValue={editing.link} placeholder="https://leetcode.com/problems/..." /></label>
          <label>Problem statement<textarea name="statement" required defaultValue={editing.statement} /></label>
          <label>Brute force approach <small>(optional)</small><textarea name="brute" defaultValue={editing.brute} /></label>
          <label>Optimal approach<textarea name="optimal" required defaultValue={editing.optimal} /></label>
          <div className="two">
            <label>Time complexity<input name="time" defaultValue={editing.time} placeholder="O(n)" /></label>
            <label>Space complexity<input name="space" defaultValue={editing.space} placeholder="O(1)" /></label>
          </div>
          <label>The trick that made it click <small>(this is what you will remember)</small><textarea name="trick" defaultValue={editing.trick} /></label>
          <label>Code <small>(optional)</small><textarea className="code" name="code" spellCheck="false" defaultValue={editing.code} /></label>
          <div className="row">
            <button className="btn" type="submit">{editId ? "Save changes" : "Save problem"}</button>
            <button className="btn sec" type="button" onClick={() => go("all")}>Cancel</button>
          </div>
        </form>
      )}
    </>
  );
}

export default function App() {
  const [authed, setAuthed] = useState(!!getToken());
  const [me, setMe] = useState(null);
  useEffect(() => {
    if (authed) api("/api/me").then(setMe).catch(() => {});
    else setMe(null);
  }, [authed]);
  return (
    <div className="wrap">
      {authed
        ? <Notebook me={me} onLogout={() => { setToken(null); setAuthed(false); }} />
        : <><Header /><Auth onDone={() => setAuthed(true)} /></>}
    </div>
  );
}
