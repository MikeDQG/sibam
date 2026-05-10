const features = [
  {
    title: "Pametno multimodalno načrtovanje poti",
    description:
      "Aplikacija uporabniku omogoča iskanje optimalne poti z uporabo različnih načinov prevoza. Sistem sam izračuna najbolj učinkovito povezavo med lokacijami.",
    items: ["peš", "Mbajk", "avtobus", "kombinacije vseh načinov"],
  },
  {
    title: "Izbira načina optimizacije poti",
    description:
      "Uporabnik lahko izbira med različnimi tipi poti glede na svoje potrebe.",
    items: [
      "Najhitrejša pot - prioriteta je čas prihoda",
      "Zelena pot - poudarek na trajnostni mobilnosti",
      "Dostopnostna pot - brez kolesa in z bolj dostopnimi potmi",
    ],
  },
  {
    title: "Shranjevanje pogostih lokacij",
    description:
      "Shranjene lokacije omogočajo hitrejše načrtovanje poti in bolj personalizirano uporabniško izkušnjo.",
    items: ["dom", "služba", "fakulteta", "priljubljene destinacije"],
  },
  {
    title: "Integracija Mbajk in avtobusnega sistema",
    description:
      "Sistem se povezuje z mestnimi podatki o mobilnosti in jih vključi neposredno v izračun poti.",
    items: [
      "pregled razpoložljivih Mbajk koles",
      "prikaz avtobusnih linij in prihodov",
      "vključitev javnega prevoza v izračun poti",
    ],
  },
];

export const FeaturesSection = () => {
  return (
    <section className='bg-neutral-50 px-6 py-20 text-neutral-900 lg:px-16 lg:py-28'>
      <div className='mx-auto flex max-w-6xl flex-col gap-16'>
        <div className='max-w-2xl'>
          <span className='text-sm font-semibold uppercase tracking-wide text-red-700'>
            Funkcionalnosti
          </span>
          <h2 className='mt-3 text-4xl font-bold leading-tight lg:text-5xl'>
            Načrtovanje poti, prilagojeno tvojemu načinu premikanja.
          </h2>
        </div>

        <div className='flex flex-col gap-20 lg:gap-28'>
          {features.map((feature, index) => {
            const isReversed = index % 2 === 1;

            return (
              <article
                key={feature.title}
                className={`grid items-center gap-8 lg:grid-cols-2 ${
                  isReversed ? "lg:[&>*:first-child]:order-2" : ""
                }`}>
                <div>
                  <span className='text-sm font-semibold text-red-700'>
                    0{index + 1}
                  </span>
                  <h3 className='mt-3 text-2xl font-bold lg:text-3xl'>
                    {feature.title}
                  </h3>
                  <p className='mt-4 text-lg leading-8 text-neutral-600'>
                    {feature.description}
                  </p>
                </div>

                <div className='rounded-lg border border-neutral-200 bg-white p-6 shadow-sm'>
                  <ul className='space-y-4'>
                    {feature.items.map((item) => (
                      <li
                        key={item}
                        className='flex items-start gap-3 text-base text-neutral-700'>
                        <span className='mt-2 h-2 w-2 shrink-0 rounded-full bg-red-700' />
                        <span>{item}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              </article>
            );
          })}
        </div>
      </div>
    </section>
  );
};
