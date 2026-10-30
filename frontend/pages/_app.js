import '../styles/globals.css';
import Navbar from '../components/Navbar';
import Head from 'next/head';

export default function MyApp({ Component, pageProps }) {
  return (
    <>
      <Head>
        <title>LedgerFlow — Institutional Multi-Branch Core Banking</title>
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <link rel="icon" href="/favicon.ico" />
      </Head>
      <Navbar />
      <main className="container" style={{ paddingBottom: '3rem' }}>
        <Component {...pageProps} />
      </main>
    </>
  );
}

