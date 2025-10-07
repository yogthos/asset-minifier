function fetchData(url) {
    return fetch(url)
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .catch(error => {
            console.error('Error fetching data:', error);
            throw error;
        });
}

const processData = (url) => {
    return fetchData(url)
        .then(result => {
            console.log('Processed data:', result);
            return result;
        });
};

// Example usage
processData('https://api.example.com/data')
    .then(data => {
        console.log('Data processed successfully:', data);
    })
    .catch(error => {
        console.error('Failed to process data:', error);
    });

async function foo() {
    const evalInfoBefore = {};
    const bestMoveUCI = evalInfoBefore?.bestmove;
}