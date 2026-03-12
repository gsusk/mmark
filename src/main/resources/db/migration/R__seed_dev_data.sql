SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE cart_item;
TRUNCATE TABLE order_item;
TRUNCATE TABLE orders;
TRUNCATE TABLE cart;
TRUNCATE TABLE image;
TRUNCATE TABLE product;
TRUNCATE TABLE category;
TRUNCATE TABLE user;
TRUNCATE TABLE inventory_transactions;

INSERT INTO category (id, name, parent_id, attribute_definitions)
VALUES (1, 'Electrónica', NULL, '[]'),
       (2, 'Libros', NULL, '[]'),
       (3, 'Ropa', NULL, '[]'),
       (4, 'Deportes', NULL, '[]');

INSERT INTO category (id, name, parent_id, attribute_definitions)
VALUES (5, 'Televisores', 1, '[]'),
       (6, 'Computadoras', 1, '[]'),
       (7, 'Teléfonos', 1, '[]'),
       (8, 'Audífonos', 1, '[]'),
       (10, 'Tablets', 1, '[]'),
       (11, 'Relojes Inteligentes', 1, '[]'),
       (12, 'Cámaras', 1, '[]'),
       (14, 'Ficción', 2, '[]'),
       (15, 'No Ficción', 2, '[]'),
       (16, 'Cómics', 2, '[]'),
       (17, 'Libros de Cocina', 2, '[]'),
       (18, 'Ropa para Niños', 3, '[]'),
       (19, 'Calzado', 3, '[]'),
       (20, 'Equipo para Exteriores', 4, '[]'),
       (21, 'Equipo de Gimnasio', 4, '[]');

INSERT INTO product (id, name, description, price, stock, category_id, created_at, brand, attributes, slug)
VALUES 
       (1, 'Smart TV Samsung OLED 65" 4K QN65S90C', 'Experimenta colores cristalinos y negros profundos con la tecnología OLED de Samsung. Este televisor de 65 pulgadas ofrece resolución 4K real, procesador Neural Quantum para un escalado impresionante y un diseño ultra delgado que se adapta a cualquier espacio moderno.', 1500.00, 10, 5, NOW(), 'Samsung', '{"resolution": "4K", "brand": "Samsung", "screen_size_inch": 65, "warranty_months": 24}', 'smart-tv-samsung-oled-65-4k-qn65s90c'),
       (2, 'Smart TV LG C3 OLED 55" 4K evo', 'El estándar de oro en televisores OLED. Negros perfectos y contraste infinito para una experiencia cinematográfica inigualable. Equipado con el procesador α9 AI Gen6 para optimizar imagen y sonido automáticamente, ideal tanto para cine como para gaming de alto rendimiento.', 1200.00, 8, 5, NOW(), 'LG', '{"resolution": "4K", "brand": "LG", "screen_size_inch": 55, "warranty_months": 24}', 'smart-tv-lg-c3-oled-55-4k-evo'),
       (3, 'Smart TV Sony Bravia XR 75" X90L Full Array LED', 'Una experiencia cinematográfica inmersiva impulsada por el Cognitive Processor XR. Disfruta de una profundidad y realismo asombrosos con colores vibrantes y un contraste preciso gracias a su panel Full Array LED. Perfecto para entusiastas del cine y dueños de PlayStation 5.', 2500.00, 5, 5, NOW(), 'Sony', '{"resolution": "4K", "brand": "Sony", "screen_size_inch": 75, "warranty_months": 12}', 'smart-tv-sony-bravia-xr-75-x90l-full-array-led'),
       (4, 'Apple MacBook Air 13" M2 Chip 256GB SSD', 'Potente y extremadamente ligero, el nuevo MacBook Air cuenta con el chip M2 de Apple. Ofrece hasta 18 horas de batería, una pantalla Liquid Retina de 13.6 pulgadas y un sistema de cámara y audio avanzado en un diseño sin ventilador completamente silencioso.', 1100.00, 15, 6, NOW(), 'Apple', '{"cpu": "M2", "brand": "Apple", "ram_gb": 8, "warranty_months": 12}', 'apple-macbook-air-13-m2-chip-256gb-ssd'),
       (5, 'PC Gaming Ultra Custom RTX 4080 i9-14900K', 'Una auténtica bestia del gaming con refrigeración líquida. Este PC ensamblado a medida cuenta con una tarjeta gráfica NVIDIA RTX 4080 y el procesador Intel Core i9-14900K, garantizando un rendimiento extremo en resolución 4K y trazado de rayos en tiempo real.', 2800.00, 4, 6, NOW(), 'Custom', '{"cpu": "Intel i9-14900K", "brand": "Custom", "ram_gb": 32, "warranty_months": 24}', 'pc-gaming-ultra-custom-rtx-4080-i9-14900k'),
       (6, 'Lenovo ThinkPad X1 Carbon Gen 11', 'La laptop de negocios definitiva. Conocida por su legendaria durabilidad y teclado excepcional, esta generación incluye procesadores Intel Core de 13ra generación, seguridad empresarial avanzada y un chasis de fibra de carbono increíblemente ligero y resistente.', 1600.00, 12, 6, NOW(), 'Lenovo', '{"cpu": "Intel i7-1365U", "brand": "Lenovo", "ram_gb": 16, "warranty_months": 36}', 'lenovo-thinkpad-x1-carbon-gen-11'),
       (7, 'iPhone 15 Pro 256GB Titanio Natural', 'Diseño de titanio de calidad aeroespacial, ligero y resistente. Cuenta con el nuevo chip A17 Pro para un rendimiento gráfico sin precedentes y un sistema de cámaras Pro versátil que redefine la fotografía móvil con su zoom óptico y capacidades de video profesional.', 1099.00, 25, 7, NOW(), 'Apple', '{"brand": "Apple", "dual_sim": false, "storage_gb": 256, "warranty_months": 12}', 'iphone-15-pro-256gb-titanio-natural'),
       (8, 'Google Pixel 8 Pro 128GB Obsidiana', 'Lo mejor de la IA de Google directamente en tu mano. El Pixel 8 Pro ofrece un sistema de triple cámara trasera líder en la industria, funciones de edición de fotos mágicas y una integración profunda con los servicios de Google para una experiencia Android pura y fluida.', 999.00, 20, 7, NOW(), 'Google', '{"brand": "Google", "dual_sim": true, "storage_gb": 128, "warranty_months": 12}', 'google-pixel-8-pro-128gb-obsidiana'),
       (9, 'Samsung Galaxy S24 Ultra 512GB Titanium Gray', 'Lleva tu productividad y creatividad al siguiente nivel con el S-Pen integrado. Pantalla plana de 6.8 pulgadas increíblemente brillante, cámara de 200MP y funciones de IA generativa que traducen llamadas en directo y buscan objetos con solo rodearlos en pantalla.', 1299.00, 18, 7, NOW(), 'Samsung', '{"brand": "Samsung", "dual_sim": true, "storage_gb": 512, "warranty_months": 24}', 'samsung-galaxy-s24-ultra-512gb-titanium-gray'),
       (10, 'Apple AirPods Pro (2.ª generación) con USB-C', 'Cancelación activa de ruido hasta dos veces más efectiva que la generación anterior. Audio espacial personalizado para una inmersión total y ahora con estuche de carga MagSafe compatible con USB-C para una mayor versatilidad en tus dispositivos.', 249.00, 40, 8, NOW(), 'Apple', '{"type": "In-ear", "brand": "Apple", "wireless": true, "warranty_months": 12}', 'apple-airpods-pro-2-generacion-con-usb-c'),
       (11, 'Sony WH-1000XM5 Audífonos Inalámbricos', 'Líderes en la industria en cancelación de ruido. Disfruta de una calidad de sonido premium y llamadas manos libres excepcionalmente claras gracias a sus múltiples micrófonos. Con una batería de hasta 30 horas y carga rápida, son el compañero de viaje perfecto.', 399.00, 15, 8, NOW(), 'Sony', '{"type": "Over-ear", "brand": "Sony", "wireless": true, "warranty_months": 12}', 'sony-wh-1000xm5-audifonos-inalambricos'),
       (12, 'Samsung Galaxy Buds2 Pro Grafito', 'Sonido de alta fidelidad de 24 bits para una experiencia auditiva envolvente. Diseño ergonómico mejorado para un ajuste cómodo y seguro, cancelación activa de ruido inteligente y resistencia al agua IPX7, ideales para hacer ejercicio o uso diario intenso.', 199.00, 30, 8, NOW(), 'Samsung', '{"type": "In-ear", "brand": "Samsung", "wireless": true, "warranty_months": 12}', 'samsung-galaxy-buds2-pro-grafito'),
       (13, 'iPad Pro 12.9" (6.ª gen) M2 256GB Wi-Fi', 'Rendimiento deslumbrante con el chip M2. La pantalla Liquid Retina XDR de 12.9 pulgadas ofrece un brillo extremo y un contraste increíble, ideal para ver y editar contenido HDR. Compatible con Apple Pencil de 2da generación y Magic Keyboard para una productividad total.', 1099.00, 10, 10, NOW(), 'Apple', '{"brand": "Apple", "storage_gb": 256, "screen_size_inch": 12.9}', 'ipad-pro-129-6-gen-m2-256gb-wi-fi'),
       (14, 'Samsung Galaxy Tab S9 11" 256GB Graphite', 'La tablet Android más potente con pantalla Dynamic AMOLED 2X. Resistente al agua y al polvo con certificación IP68, incluye el S-Pen en la caja para dibujar y tomar notas con una latencia ultra baja. Perfecta para entretenimiento y trabajo creativo en movimiento.', 899.00, 12, 10, NOW(), 'Samsung', '{"brand": "Samsung", "storage_gb": 256, "screen_size_inch": 11}', 'samsung-galaxy-tab-s9-11-256gb-graphite'),
       (15, 'Apple Watch Series 9 45mm GPS Caja Medianoche', 'Más potente que nunca gracias al chip S9 SiP. La nueva función de doble toque permite interactuar sin tocar la pantalla. Monitoreo avanzado de salud con sensor de oxígeno en sangre, ECG y notificaciones de ritmo cardíaco irregular en un diseño icónico.', 399.00, 20, 11, NOW(), 'Apple', '{"brand": "Apple", "color": "Medianoche", "warranty_months": 12}', 'apple-watch-series-9-45mm-gps-caja-medianoche'),
       (16, 'Garmin fēnix 7 Sapphire Solar Edition', 'Reloj GPS multideporte de alto rendimiento con carga solar para extender la autonomía de la batería. Incluye mapas preinstalados, métricas de entrenamiento avanzadas y funciones de seguimiento de salud 24/7 en un diseño robusto y elegante con cristal de zafiro.', 699.00, 8, 11, NOW(), 'Garmin', '{"brand": "Garmin", "color": "Negro", "warranty_months": 12}', 'garmin-fenix-7-sapphire-solar-edition'),
       (17, 'Sony Alpha 7 IV Cámara Mirrorless Full Frame', 'La cámara híbrida ideal para creadores de contenido. Con una resolución de 33MP, enfoque automático líder en su clase y grabación de video 4K 60p, ofrece un equilibrio perfecto entre fotografía profesional y producción de video de alta calidad.', 2499.00, 5, 12, NOW(), 'Sony', '{"brand": "Sony", "megapixels": 33, "sensor_type": "Full Frame"}', 'sony-alpha-7-iv-camara-mirrorless-full-frame'),
       (18, 'Fujifilm X-T5 Cuerpo Negro', 'Cámara mirrorless compacta con un sensor X-Trans CMOS 5 HR de 40.2MP. Mantiene los controles manuales clásicos de Fujifilm, ofreciendo una calidad de imagen excepcional y una portabilidad inigualable para entusiastas y profesionales del reportaje y viajes.', 1699.00, 7, 12, NOW(), 'Fujifilm', '{"brand": "Fujifilm", "megapixels": 40.2, "sensor_type": "APS-C"}', 'fujifilm-x-t5-cuerpo-negro'),
       (19, 'Project Hail Mary - Andy Weir (Edición en Inglés)', 'Del autor de "El Marciano", una nueva aventura épica de supervivencia espacial. Ryland Grace se despierta en una nave estelar sin recordar quién es, pero pronto descubre que él es la única esperanza de la humanidad en una misión desesperada hacia las estrellas.', 18.00, 50, 14, NOW(), 'Ballantine', '{"author": "Andy Weir", "genre": "Sci-Fi"}', 'project-hail-mary-andy-weir-edicion-en-ingles'),
       (20, 'The Midnight Library - Matt Haig', 'Entre la vida y la muerte hay una biblioteca, y en sus estantes hay libros que contienen la historia de las vidas que podrías haber vivido. Una novela fascinante sobre las decisiones que tomamos y cómo encontrar la felicidad en la vida que realmente tenemos.', 15.00, 60, 14, NOW(), 'Viking', '{"author": "Matt Haig", "genre": "Fantasy"}', 'the-midnight-library-matt-haig'),
       (21, 'Hábitos Atómicos - James Clear', 'Un método sencillo y comprobado para desarrollar buenos hábitos y eliminar los malos. Basado en la psicología y la neurociencia, James Clear explica cómo pequeños cambios cotidianos pueden llevar a resultados extraordinarios a largo plazo.', 20.00, 100, 15, NOW(), 'Avery', '{"author": "James Clear", "genre": "Autoayuda"}', 'habitos-atomicos-james-clear'),
       (22, 'Sapiens: De animales a dioses - Yuval Noah Harari', 'Un relato apasionante de la historia de nuestra especie, desde los primeros humanos que caminaron sobre la Tierra hasta los radicales y a veces devastadores avances de las revoluciones cognitiva, agrícola y científica. Un libro que cambiará tu forma de ver el mundo.', 22.00, 80, 15, NOW(), 'Harper', '{"author": "Yuval Noah Harari", "genre": "Historia"}', 'sapiens-de-animales-a-dioses-yuval-noah-harari'),
       (23, 'Watchmen Edición de Lujo - Alan Moore & Dave Gibbons', 'La novela gráfica que revolucionó el género de los superhéroes. Una obra maestra narrativa y visual que explora temas complejos de poder, moralidad y el fin de la esperanza en una realidad alternativa donde los vigilantes disfrazados han cambiado la historia.', 35.00, 30, 16, NOW(), 'DC Comics', '{"author": "Alan Moore", "genre": "Superhéroes"}', 'watchmen-edicion-de-lujo-alan-moore-dave-gibbons'),
       (24, 'Saga Vol. 1 - Brian K. Vaughan & Fiona Staples', 'Una épica de ciencia ficción y fantasía sobre una pareja de soldados de bandos opuestos que se enamoran y deben huir a través de la galaxia para proteger a su hija recién nacida. Una historia visualmente impactante y emocionalmente resonante.', 10.00, 45, 16, NOW(), 'Image Comics', '{"author": "Brian K. Vaughan", "genre": "Sci-Fi"}', 'saga-vol-1-brian-k-vaughan-fiona-staples'),
       (25, 'Camiseta Básica Algodón Orgánico Hombre Blanca', 'Camiseta de manga corta confeccionada en algodón 100% orgánico certificado. Ofrece un tacto suave y una transpirabilidad superior para el máximo confort diario. Un básico esencial con un corte moderno que nunca pasa de moda.', 25.00, 50, 3, NOW(), 'Generic', '{"size": "L", "material": "Algodón"}', 'camiseta-basica-algodon-organico-hombre-blanca'),
       (26, 'Chaqueta Denim Clásica Levi''s Trucker', 'La chaqueta vaquera original desde 1967. Confeccionada en mezclilla de alta calidad que mejora con el tiempo, cuenta con el corte clásico americano, botones metálicos y los bolsillos frontales icónicos de Levi''s. Un símbolo de estilo individual.', 85.00, 20, 3, NOW(), 'Levis', '{"size": "M", "material": "Mezclilla"}', 'chaqueta-denim-clasica-levis-trucker'),
       (27, 'Jeans Wrangler Slim Fit Corte Moderno', 'Vaqueros de corte ajustado diseñados para ofrecer un look actual sin sacrificar la comodidad. Fabricados con mezclilla elástica que se adapta a tus movimientos, son la elección perfecta para un estilo casual y versátil en cualquier ocasión.', 65.00, 35, 3, NOW(), 'Wrangler', '{"size": "S", "material": "Mezclilla"}', 'jeans-wrangler-slim-fit-corte-moderno'),
       (28, 'Zapatillas Nike Air Max 270 Hombre Negro/Blanco', 'Inspiradas en dos iconos de los archivos de Nike, estas zapatillas cuentan con la unidad de aire más grande en el talón para una pisada increíblemente suave. El ajuste tipo botín garantiza comodidad y estabilidad durante todo el día con un estilo urbano audaz.', 160.00, 40, 19, NOW(), 'Nike', '{"size": "10", "material": "Malla"}', 'zapatillas-nike-air-max-270-hombre-negro-blanco'),
       (29, 'Adidas Ultraboost Light Running Hombre Azules', 'La Ultraboost más ligera de la historia con el nuevo material Light BOOST. Siente una energía épica en cada kilómetro con una amortiguación reactiva y un ajuste superior Primeknit que envuelve el pie proporcionando soporte y transpirabilidad.', 180.00, 25, 19, NOW(), 'Adidas', '{"size": "9", "material": "Textil"}', 'adidas-ultraboost-light-running-hombre-azules'),
       (30, 'Botas Timberland 6-Inch Premium Impermeables Amarillas', 'La bota clásica que lo empezó todo hace más de 40 años. Fabricadas en cuero nobuck de alta calidad con costuras selladas para una impermeabilidad total. Duraderas, cómodas y reconocidas en todo el mundo por su estilo atemporal.', 198.00, 15, 19, NOW(), 'Timberland', '{"size": "11", "material": "Cuero"}', 'botas-timberland-6-inch-premium-impermeables-amarillas'),
       (31, 'Mochila Técnica The North Face Terra 65L', 'Diseñada para largas travesías en la montaña. Cuenta con el sistema de suspensión OPTIFIT para un ajuste seguro y cómodo, múltiples bolsillos de acceso rápido y una durabilidad extrema para soportar las condiciones más exigentes al aire libre.', 180.00, 10, 20, NOW(), 'The North Face', '{"weight_kg": 2.1, "sport_type": "Senderismo"}', 'mochila-tecnica-the-north-face-terra-65l'),
       (32, 'Chaqueta Impermeable Columbia Watertight II Hombre', 'Chaqueta ligera y versátil con tecnología Omni-Tech que ofrece protección impermeable y transpirable. Se puede plegar y guardar en su propio bolsillo, siendo ideal para llevar en la mochila ante cambios inesperados de clima en tus aventuras.', 75.00, 30, 20, NOW(), 'Columbia', '{"weight_kg": 0.4, "sport_type": "Outdoor"}', 'chaqueta-impermeable-columbia-watertight-ii-hombre'),
       (33, 'Pesa Rusa Kettlebell 16kg Hierro Fundido', 'Equipo esencial para entrenamiento funcional y de fuerza. Fabricada en hierro fundido macizo con un acabado duradero y un agarre ergonómico que permite una amplia variedad de ejercicios para mejorar la potencia, la coordinación y la resistencia cardiovascular.', 45.00, 40, 21, NOW(), 'Bowflex', '{"weight_kg": 16, "sport_type": "Fitness"}', 'pesa-rusa-kettlebell-16kg-hierro-fundido'),
       (34, 'Mancuernas Ajustables Bowflex SelectTech 552 (Par)', 'El sistema de pesas más versátil para tu gimnasio en casa. Sustituye hasta 15 pares de mancuernas individuales con un solo giro de dial. Permite ajustar el peso desde 2kg hasta 24kg, optimizando espacio y ofreciendo un entrenamiento de fuerza completo.', 350.00, 15, 21, NOW(), 'Bowflex', '{"weight_kg": 24, "sport_type": "Fitness"}', 'mancuernas-ajustables-bowflex-selecttech-552-par'),
       (35, 'Audífonos Bose QuietComfort Ultra Over-Ear', 'La cúspide del audio con cancelación de ruido. Bose redefine la escucha con audio inmersivo que coloca el sonido frente a ti, sin importar la fuente. Comodidad de lujo y materiales premium para sesiones de escucha prolongadas sin fatiga.', 429.00, 12, 8, NOW(), 'Bose', '{"type": "Over-ear", "wireless": true}', 'audifonos-bose-quietcomfort-ultra-over-ear'),
       (36, 'Nintendo Switch Modelo OLED Blanco', 'Mejora tu experiencia de juego con una pantalla OLED de 7 pulgadas con colores vibrantes y contraste nítido. Incluye un soporte ajustable ancho, una base con puerto LAN por cable y 64GB de almacenamiento interno para disfrutar de tus juegos favoritos donde quieras.', 349.00, 30, 6, NOW(), 'Nintendo', '{"brand": "Nintendo", "warranty_months": 12}', 'nintendo-switch-modelo-oled-blanco'),
       (37, 'Mouse Inalámbrico Logitech MX Master 3S Grafito', 'El mouse de alto desempeño más avanzado para profesionales. Ahora con clics discretos y un sensor óptico de 8,000 DPI que funciona incluso sobre cristal. Diseñado para ofrecer precisión, velocidad y una ergonomía inigualable durante largas jornadas de trabajo.', 99.00, 50, 6, NOW(), 'Logitech', '{"brand": "Logitech", "type": "Mouse"}', 'mouse-inalambrico-logitech-mx-master-3s-grafito'),
       (38, 'Teclado Mecánico Razer BlackWidow V4 Pro', 'Un teclado gaming de élite con interruptores mecánicos Razer para una ejecución rápida y precisa. Cuenta con una iluminación subyacente envolvente, teclas multimedia dedicadas y un dial de control personalizable para tener todo el mando de tu setup.', 169.00, 25, 6, NOW(), 'Razer', '{"brand": "Razer", "type": "Keyboard"}', 'teclado-mecanico-razer-blackwidow-v4-pro'),
       (39, 'Aspiradora Inalámbrica Dyson V15 Detect Absolute', 'La aspiradora sin cable más potente e inteligente de Dyson. Cuenta con una luz que revela el polvo invisible en suelos duros y una pantalla LCD que muestra pruebas científicas de una limpieza profunda en tiempo real. Máxima succión para todo tipo de superficies.', 749.00, 10, 1, NOW(), 'Dyson', '{"brand": "Dyson", "type": "Aspiradora"}', 'aspiradora-inalambrica-dyson-v15-detect-absolute'),
       (40, 'Horno Holandés Le Creuset Hierro Fundido 5.5 qt', 'Un clásico indispensable en la cocina. El hierro fundido vitrificado garantiza una distribución y retención del calor uniformes para cocciones lentas perfectas. Su interior esmaltado es resistente a manchas y fácil de limpiar. Apto para todas las fuentes de calor.', 380.00, 15, 17, NOW(), 'Le Creuset', '{"material": "Hierro Fundido", "size": "5.5 qt"}', 'horno-holandes-le-creuset-hierro-fundido-55-qt'),
       (41, 'Freidora de Aire Ninja Foodi 6 en 1 8-qt (DualZone)', 'Cocina dos comidas de dos maneras diferentes y termina al mismo tiempo con la tecnología DualZone. Gran capacidad de 8 cuartos ideal para familias, con funciones para freír con aire, asar, deshidratar y más, usando hasta un 75% menos de grasa.', 199.00, 22, 1, NOW(), 'Ninja', '{"brand": "Ninja", "capacity": "8 qt"}', 'freidora-de-aire-ninja-foodi-6-en-1-8-qt-dualzone');

INSERT INTO image (product_id, url, position)
VALUES (1, 'https://images.unsplash.com/photo-1593359677879-a4bb92f829d1?auto=format&fit=crop&q=80&w=1500', 0),
       (2, 'https://images.unsplash.com/photo-1593359677879-a4bb92f829d1?auto=format&fit=crop&q=80&w=1500', 0),
       (3, 'https://images.unsplash.com/photo-1509281373149-e957c6296406?auto=format&fit=crop&q=80&w=1500', 0),
       (4, 'https://images.unsplash.com/photo-1611186871348-b1ce696e52c9?auto=format&fit=crop&q=80&w=1500', 0),
       (5, 'https://images.unsplash.com/photo-1587202372775-e229f172b9d7?auto=format&fit=crop&q=80&w=1500', 0),
       (6, 'https://images.unsplash.com/photo-1743456056112-0739a6742135?auto=format&fit=crop&q=80&w=1500', 0),
       (7, 'https://images.unsplash.com/photo-1696446701796-da61225697cc?auto=format&fit=crop&q=80&w=1500', 0),
       (8, 'https://images.unsplash.com/photo-1711027466894-c2bd5da009c3?auto=format&fit=crop&q=80&w=1500', 0),
       (9, 'https://images.unsplash.com/photo-1705585174953-9b2aa8afc174?auto=format&fit=crop&q=80&w=1500', 0),
       (10, 'https://images.unsplash.com/photo-1603351154351-5e2d0600bb77?auto=format&fit=crop&q=80&w=1500', 0),
       (11, 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&q=80&w=1500', 0),
       (12, 'https://images.unsplash.com/photo-1590658268037-6bf12165a8df?auto=format&fit=crop&q=80&w=1500', 0),
       (13, 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?auto=format&fit=crop&q=80&w=1500', 0),
       (14, 'https://images.unsplash.com/photo-1561154464-82e9adf32764?auto=format&fit=crop&q=80&w=1500', 0),
       (15, 'https://images.unsplash.com/photo-1624096104992-9b4fa3a279dd?auto=format&fit=crop&q=80&w=1500', 0),
       (16, 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&q=80&w=1500', 0),
       (17, 'https://images.unsplash.com/photo-1516035069371-29a1b244cc32?auto=format&fit=crop&q=80&w=1500', 0),
       (18, 'https://images.unsplash.com/photo-1520390138845-fd2d229dd553?auto=format&fit=crop&q=80&w=1500', 0),
       (19, 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&q=80&w=1500', 0),
       (20, 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&q=80&w=1500', 0),
       (21, 'https://images.unsplash.com/photo-1589998059171-988d887df646?auto=format&fit=crop&q=80&w=1500', 0),
       (22, 'https://images.unsplash.com/photo-1476275466078-4007374efbbe?auto=format&fit=crop&q=80&w=1500', 0),
       (23, 'https://images.unsplash.com/photo-1541963463532-d68292c34b19?auto=format&fit=crop&q=80&w=1500', 0),
       (24, 'https://m.media-amazon.com/images/I/71cVDK9BRaL._SL1500_.jpg', 0),
       (25, 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&q=80&w=1500', 0),
       (25, 'https://images.unsplash.com/photo-1576417677416-6ca3adfb5435?q=80&w=1270&auto=format&fit=crop', 1),
       (26, 'https://images.unsplash.com/photo-1551028719-00167b16eac5?auto=format&fit=crop&q=80&w=1500', 0),
       (27, 'https://images.unsplash.com/photo-1542272604-787c3835535d?auto=format&fit=crop&q=80&w=1500', 0),
       (28, 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&q=80&w=1500', 0),
       (29, 'https://images.unsplash.com/photo-1539185441755-769473a23570?auto=format&fit=crop&q=80&w=1500', 0),
       (30, 'https://m.media-amazon.com/images/I/71lGBGsXEML._AC_SY395_SX395_QL70_ML2_.jpg', 0),
       (31, 'https://images.unsplash.com/photo-1523983388277-336a66bf9bcd?auto=format&fit=crop&q=80&w=1500', 0),
       (32, 'https://m.media-amazon.com/images/I/71W7A++NqCL._AC_SX342_SY445_QL70_ML2_.jpg', 0),
       (33, 'https://images.unsplash.com/photo-1583454110551-21f2fa2afe61?auto=format&fit=crop&q=80&w=1500', 0),
       (34, 'https://images.unsplash.com/photo-1584735935682-2f2b69dff9d2?auto=format&fit=crop&q=80&w=1500', 0),
       (35, 'https://images.unsplash.com/photo-1546435770-a3e426bf472b?auto=format&fit=crop&q=80&w=1500', 0),
       (36, 'https://images.unsplash.com/photo-1578303512597-81e6cc155b3e?auto=format&fit=crop&q=80&w=1500', 0),
       (37, 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?auto=format&fit=crop&q=80&w=1500', 0),
       (38, 'https://images.unsplash.com/photo-1511467687858-23d96c32e4ae?auto=format&fit=crop&q=80&w=1500', 0),
       (39, 'https://images.unsplash.com/photo-1558317374-067fb5f30001?auto=format&fit=crop&q=80&w=1500', 0),
       (40, 'https://images.unsplash.com/photo-1584346133934-a3afd2a33c4c?auto=format&fit=crop&q=80&w=1500', 0),
       (41, 'https://images.unsplash.com/photo-1547032175-7fc8c7bd15b3?auto=format&fit=crop&q=80&w=1500', 0);

INSERT INTO user (id, name, last_name, email, password, role, created_at)
VALUES (1, 'Mario', 'Contreras', 'marioc@gmail.com', 'password123', 'USER', NOW()),
       (2, 'Laura', 'Gomez', 'laura@gmail.com', 'password123', 'USER', NOW()),
       (3, 'Andres', 'Lopez', 'andres@gmail.com', 'password123', 'USER', NOW());

INSERT INTO cart (id, user_id, guest_id, status, created_at)
VALUES (1, 1, NULL, 'ACTIVE', NOW()),
       (2, 2, NULL, 'ACTIVE', ADDDATE(NOW(), -10));

INSERT INTO cart_item (cart_id, product_id, unit_price, quantity, created_at)
VALUES (1, 1, 1500.00, 1, NOW()),
       (2, 6, 1600.00, 1, NOW());

INSERT INTO inventory_transactions (product_id, quantity, type, reason, created_at)
SELECT id, stock, 'RESTOCK', 'seed inicial', NOW()
FROM product;

SET FOREIGN_KEY_CHECKS = 1;
